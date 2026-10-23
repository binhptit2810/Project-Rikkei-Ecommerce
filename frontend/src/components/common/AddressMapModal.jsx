import React, { useState, useEffect, useRef, useCallback } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  Search,
  MapPin,
  Navigation,
  X,
  AlertCircle,
  Check,
  Compass,
  Loader2,
  ChevronRight,
  Info,
  Layers,
  Map,
  Globe,
} from 'lucide-react'
import mapService, {
  DEFAULT_CENTER,
  PRESET_LOCATIONS,
  searchAddress,
  reverseGeocode,
} from '../../services/mapService'

/**
 * Các lớp bản đồ chi tiết, rõ nét (Google Maps Street, Satellite, Terrain)
 * Giải quyết triệt để tình trạng bản đồ bị nhạt/nền trắng mờ
 */
const MAP_LAYERS = {
  streets: {
    id: 'streets',
    name: 'Đường phố rõ nét',
    url: 'https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}',
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    maxZoom: 20,
    attribution: '&copy; Google Maps',
  },
  satellite: {
    id: 'satellite',
    name: 'Vệ tinh ảnh chụp',
    url: 'https://{s}.google.com/vt/lyrs=y&x={x}&y={y}&z={z}',
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    maxZoom: 20,
    attribution: '&copy; Google Maps',
  },
  terrain: {
    id: 'terrain',
    name: 'Địa hình giao thông',
    url: 'https://{s}.google.com/vt/lyrs=p&x={x}&y={y}&z={z}',
    subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
    maxZoom: 20,
    attribution: '&copy; Google Maps',
  },
}

/**
 * Tạo Icon ghim vị trí SVG hiện đại với hiệu ứng xung nhịp radar
 */
const createCustomMarkerIcon = () => {
  return L.divIcon({
    className: 'custom-leaflet-marker',
    html: `
      <div style="position: relative; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;">
        <span style="position: absolute; width: 100%; height: 100%; border-radius: 9999px; background-color: rgba(234, 88, 12, 0.4); animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;"></span>
        <div style="position: relative; width: 36px; height: 36px; background: #ea580c; border: 2.5px solid #ffffff; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.35); display: flex; align-items: center; justify-content: center;">
          <div style="transform: rotate(45deg); display: flex; align-items: center; justify-content: center;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
              <circle cx="12" cy="10" r="3"/>
            </svg>
          </div>
        </div>
      </div>
    `,
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -42],
  })
}

export const AddressMapModal = ({
  isOpen,
  onClose,
  onConfirmAddress,
  initialAddress = '',
}) => {
  const mapContainerRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRef = useRef(null)
  const tileLayerRef = useRef(null)

  // Kiểu hiển thị bản đồ: 'streets' (mặc định - đường nét rõ ràng), 'satellite' (ảnh chụp vệ tinh thực tế), 'terrain'
  const [activeMapType, setActiveMapType] = useState('streets')

  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [isSearching, setIsSearching] = useState(false)
  const [isGeocoding, setIsGeocoding] = useState(false)
  const [isLocatingUser, setIsLocatingUser] = useState(false)

  // Thông báo lỗi tìm kiếm theo yêu cầu: "nếu ko có thì báo địa chỉ ko đúng"
  const [searchError, setSearchError] = useState('')

  // Trạng thái địa chỉ đang được chọn
  const [selectedCoords, setSelectedCoords] = useState(null)
  const [selectedAddress, setSelectedAddress] = useState('')
  const [editableAddress, setEditableAddress] = useState('')

  // Khởi tạo tọa độ ban đầu
  const initCoordsRef = useRef({
    lat: DEFAULT_CENTER.lat,
    lon: DEFAULT_CENTER.lon,
  })

  // Đóng modal khi nhấn phím Escape
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
    }
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  // Reset form khi mở modal
  useEffect(() => {
    if (isOpen) {
      setSearchError('')
      setSearchResults([])
      if (initialAddress && initialAddress.trim()) {
        setSearchQuery(initialAddress.trim())
        setEditableAddress(initialAddress.trim())
        setSelectedAddress(initialAddress.trim())
      } else {
        setSearchQuery('')
        setEditableAddress('')
        setSelectedAddress('')
      }
    }
  }, [isOpen, initialAddress])

  /**
   * Cập nhật địa chỉ trực tiếp từ tọa độ Lat/Lon
   */
  const handleSelectCoordinates = useCallback(
    async (lat, lon, shouldFly = false, customText = null) => {
      setSelectedCoords({ lat, lon })
      setIsGeocoding(true)
      setSearchError('')

      if (mapInstanceRef.current && markerRef.current) {
        markerRef.current.setLatLng([lat, lon])
        if (shouldFly) {
          mapInstanceRef.current.flyTo([lat, lon], 16, { duration: 1.2 })
        }
      }

      try {
        const geoResult = await reverseGeocode(lat, lon)
        const finalAddress = customText || geoResult.formattedAddress
        setSelectedAddress(finalAddress)
        setEditableAddress(finalAddress)

        // Cập nhật popup trên marker
        if (markerRef.current) {
          markerRef.current
            .bindPopup(
              `<div style="font-family: inherit; font-size: 12px; line-height: 1.4; max-width: 240px; padding: 2px;">
                <strong style="color: #ea580c; display: block; margin-bottom: 3px;">📍 Vị trí đã chọn</strong>
                <span style="color: #334155;">${finalAddress}</span>
              </div>`,
              { closeButton: false }
            )
            .openPopup()
        }
      } catch (err) {
        console.error('Lỗi khi phân giải địa chỉ:', err)
        const fallback = customText || `Tọa độ: ${lat.toFixed(5)}, ${lon.toFixed(5)}`
        setSelectedAddress(fallback)
        setEditableAddress(fallback)
      } finally {
        setIsGeocoding(false)
      }
    },
    []
  )

  /**
   * Chuyển đổi lớp bản đồ (Đường phố rõ nét / Vệ tinh thực tế / Địa hình)
   */
  const handleSwitchMapType = (typeKey) => {
    setActiveMapType(typeKey)
    if (!mapInstanceRef.current) return
    const config = MAP_LAYERS[typeKey] || MAP_LAYERS.streets

    if (tileLayerRef.current) {
      mapInstanceRef.current.removeLayer(tileLayerRef.current)
    }

    const newLayer = L.tileLayer(config.url, {
      attribution: config.attribution,
      maxZoom: config.maxZoom,
      subdomains: config.subdomains,
    }).addTo(mapInstanceRef.current)

    newLayer.bringToBack()
    tileLayerRef.current = newLayer
  }

  /**
   * Khởi tạo hoặc cập nhật Leaflet Map
   */
  useEffect(() => {
    if (!isOpen) {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        markerRef.current = null
        tileLayerRef.current = null
      }
      return
    }

    // Đợi DOM render
    const timer = setTimeout(() => {
      if (!mapContainerRef.current) return

      if (!mapInstanceRef.current) {
        const map = L.map(mapContainerRef.current, {
          center: [initCoordsRef.current.lat, initCoordsRef.current.lon],
          zoom: DEFAULT_CENTER.zoom,
          zoomControl: false,
        })

        // Nút zoom ở góc dưới bên phải
        L.control.zoom({ position: 'bottomright' }).addTo(map)

        // Lớp bản đồ Google Maps sắc nét, đường xá và địa danh cực rõ ràng
        const initialConfig = MAP_LAYERS[activeMapType] || MAP_LAYERS.streets
        const tileLayer = L.tileLayer(initialConfig.url, {
          attribution: initialConfig.attribution,
          maxZoom: initialConfig.maxZoom,
          subdomains: initialConfig.subdomains,
        }).addTo(map)
        tileLayerRef.current = tileLayer

        // Tạo ghim vị trí tùy chỉnh có thể kéo thả
        const marker = L.marker(
          [initCoordsRef.current.lat, initCoordsRef.current.lon],
          {
            icon: createCustomMarkerIcon(),
            draggable: true,
          }
        ).addTo(map)

        // Sự kiện người dùng nhấp trực tiếp lên bản đồ
        map.on('click', (e) => {
          const { lat, lng } = e.latlng
          handleSelectCoordinates(lat, lng, false)
        })

        // Sự kiện khi người dùng kéo thả marker đến điểm mới
        marker.on('dragend', () => {
          const { lat, lng } = marker.getLatLng()
          handleSelectCoordinates(lat, lng, false)
        })

        mapInstanceRef.current = map
        markerRef.current = marker

        // Tự động phân giải vị trí mặc định nếu chưa có
        handleSelectCoordinates(
          initCoordsRef.current.lat,
          initCoordsRef.current.lon,
          false
        )

        // Nếu ban đầu người dùng đã có chuỗi địa chỉ thì tự động tìm kiếm vị trí đó
        if (initialAddress && initialAddress.trim().length >= 4) {
          executeSearch(initialAddress.trim())
        }
      }

      // Đảm bảo map render full size khi mở modal
      mapInstanceRef.current.invalidateSize()
    }, 150)

    return () => clearTimeout(timer)
  }, [isOpen, handleSelectCoordinates, initialAddress])

  /**
   * Xử lý tìm kiếm địa chỉ
   */
  const executeSearch = async (queryToSearch) => {
    const q = (queryToSearch || searchQuery).trim()
    if (!q) {
      setSearchError('Vui lòng nhập tên đường, địa chỉ hoặc khu vực cần tìm kiếm')
      return
    }

    setIsSearching(true)
    setSearchError('')
    setSearchResults([])

    try {
      const curLat = selectedCoords?.lat || initCoordsRef.current.lat
      const curLon = selectedCoords?.lon || initCoordsRef.current.lon
      const results = await searchAddress(q, curLat, curLon)

      if (results && results.length > 0) {
        setSearchResults(results)
        // Tự động chọn kết quả phù hợp nhất đầu tiên
        const top = results[0]
        await handleSelectCoordinates(top.lat, top.lon, true, top.formattedAddress)
      } else {
        // YÊU CẦU: "và nếu ko có thì báo địa chỉ ko đúng"
        setSearchError('Địa chỉ không đúng hoặc không tìm thấy trên bản đồ! Vui lòng kiểm tra lại chính tả hoặc chọn vị trí trực tiếp.')
      }
    } catch (err) {
      console.error('Search address error:', err)
      setSearchError('Không thể tìm kiếm địa chỉ lúc này. Vui lòng thử lại hoặc chọn vị trí trên bản đồ.')
    } finally {
      setIsSearching(false)
    }
  }

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    executeSearch(searchQuery)
  }

  /**
   * Lấy vị trí GPS hiện tại của người dùng
   */
  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) {
      setSearchError('Trình duyệt của bạn không hỗ trợ định vị GPS')
      return
    }

    setIsLocatingUser(true)
    setSearchError('')

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocatingUser(false)
        const { latitude, longitude } = pos.coords
        handleSelectCoordinates(latitude, longitude, true)
      },
      (err) => {
        setIsLocatingUser(false)
        console.warn('Geolocation error:', err)
        let msg = 'Không thể lấy vị trí hiện tại của bạn.'
        if (err.code === 1) {
          msg = 'Vui lòng cấp quyền truy cập vị trí trong cài đặt trình duyệt để sử dụng tính năng này.'
        }
        setSearchError(msg)
      },
      { timeout: 10000, enableHighAccuracy: true }
    )
  }

  /**
   * Chọn nhanh thành phố lớn
   */
  const handleSelectPreset = (preset) => {
    setSearchError('')
    setSearchResults([])
    handleSelectCoordinates(preset.lat, preset.lon, true)
  }

  /**
   * Xác nhận địa chỉ đã chọn và chuyển ra form đặt hàng
   */
  const handleConfirm = () => {
    const finalAddress = editableAddress.trim() || selectedAddress.trim()
    if (!finalAddress) {
      setSearchError('Vui lòng chọn hoặc nhập địa chỉ hợp lệ trước khi xác nhận')
      return
    }

    if (onConfirmAddress) {
      onConfirmAddress({
        address: finalAddress,
        coords: selectedCoords,
      })
    }
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-4xl rounded-2xl sm:rounded-3xl shadow-2xl border border-slate-200/80 overflow-hidden flex flex-col h-[90vh] max-h-[720px] min-h-[460px]">
        {/* Modal Header */}
        <div className="shrink-0 px-4 sm:px-5 py-3 sm:py-3.5 border-b border-slate-100 flex items-center justify-between bg-gradient-to-r from-slate-50 via-white to-orange-50/30">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-orange-500 text-white flex items-center justify-center shadow-xs shrink-0">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-slate-900 leading-tight">
                Chọn địa chỉ giao hàng trên bản đồ
              </h3>
              <p className="text-[11px] sm:text-xs text-slate-500 mt-0.5">
                Nhấp hoặc kéo thả ghim đến vị trí chính xác của bạn
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-100 flex items-center justify-center transition-colors cursor-pointer"
            title="Đóng (Esc)"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search Bar & Quick Location Filter (Có relative z-20 để dropdown suggestions nổi lên trên) */}
        <div className="relative shrink-0 p-3 sm:p-3.5 border-b border-slate-100 bg-white space-y-2 z-20">
          <form onSubmit={handleSearchSubmit} className="relative flex gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value)
                  if (searchError) setSearchError('')
                }}
                placeholder="Tìm kiếm địa chỉ, tên đường, khu vực... (VD: Keangnam, Landmark 81, Chợ Bến Thành)"
                autoComplete="off"
                className={`w-full pl-10 pr-9 py-2 rounded-xl text-xs sm:text-sm border transition-all ${
                  searchError
                    ? 'border-red-300 focus:ring-red-400 focus:border-red-500 bg-red-50/20'
                    : 'border-slate-200 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 bg-slate-50/50 focus:bg-white'
                } focus:outline-none text-slate-800 placeholder:text-slate-400`}
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery('')
                    setSearchResults([])
                    setSearchError('')
                  }}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            <button
              type="submit"
              disabled={isSearching || !searchQuery.trim()}
              className="px-3.5 sm:px-5 py-2 bg-orange-600 hover:bg-orange-700 disabled:opacity-50 text-white rounded-xl text-xs sm:text-sm font-semibold transition-all shadow-2xs hover:shadow-xs active:scale-[0.99] flex items-center gap-1.5 cursor-pointer disabled:cursor-not-allowed shrink-0"
            >
              {isSearching ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang tìm...</span>
                </>
              ) : (
                <>
                  <Search className="w-4 h-4" />
                  <span>Tìm kiếm</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={handleGetCurrentLocation}
              disabled={isLocatingUser}
              title="Sử dụng vị trí GPS hiện tại của bạn"
              className="px-3 sm:px-3.5 py-2 bg-slate-100 hover:bg-slate-200 active:scale-[0.98] text-slate-700 rounded-xl text-xs font-semibold transition-all flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shrink-0"
            >
              {isLocatingUser ? (
                <Loader2 className="w-4 h-4 animate-spin text-orange-600" />
              ) : (
                <Navigation className="w-4 h-4 text-orange-600" />
              )}
              <span className="hidden sm:inline">Vị trí của tôi</span>
            </button>
          </form>

          {/* Preset Location Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5 text-xs text-slate-500">
            <span className="text-[11px] font-semibold text-slate-400 shrink-0 flex items-center gap-1">
              <Compass className="w-3.5 h-3.5" />
              Khu vực:
            </span>
            {PRESET_LOCATIONS.map((preset) => (
              <button
                key={preset.name}
                type="button"
                onClick={() => handleSelectPreset(preset)}
                className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-orange-50 hover:text-orange-700 text-slate-700 text-[11px] font-medium transition-colors shrink-0 cursor-pointer"
              >
                {preset.name}
              </button>
            ))}
          </div>

          {/* Thông báo lỗi khi tìm kiếm không ra: "và nếu ko có thì báo địa chỉ ko đúng" */}
          {searchError && (
            <div className="p-2 bg-red-50 border border-red-200 rounded-xl flex items-start gap-2 text-xs text-red-700 animate-in fade-in duration-150">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
              <div className="flex-1 font-medium leading-relaxed">
                {searchError}
              </div>
            </div>
          )}

          {/* Danh sách gợi ý kết quả dạng Floating Overlay (không chiếm chiều cao modal) */}
          {searchResults.length > 1 && (
            <div className="absolute left-3 right-3 sm:left-3.5 sm:right-3.5 top-[calc(100%-4px)] z-40 bg-white/98 backdrop-blur-md border border-slate-200 rounded-2xl shadow-xl p-2 max-h-48 overflow-y-auto space-y-1 animate-in fade-in slide-in-from-top-1 duration-150">
              <div className="flex items-center justify-between text-[11px] font-semibold text-slate-500 px-2 py-1 border-b border-slate-100">
                <span>Tìm thấy {searchResults.length} địa điểm phù hợp:</span>
                <button
                  type="button"
                  onClick={() => setSearchResults([])}
                  className="text-slate-400 hover:text-slate-600 cursor-pointer text-[10px]"
                >
                  Đóng ✕
                </button>
              </div>
              {searchResults.map((res) => (
                <button
                  key={res.id}
                  type="button"
                  onClick={() => {
                    handleSelectCoordinates(res.lat, res.lon, true, res.formattedAddress)
                    setSearchResults([])
                  }}
                  className="w-full text-left px-2.5 py-2 rounded-xl text-xs hover:bg-orange-50 hover:text-orange-950 transition-colors flex items-center justify-between group cursor-pointer"
                >
                  <div className="truncate flex-1 pr-2">
                    <span className="font-bold text-slate-800 group-hover:text-orange-900 block truncate">
                      {res.name}
                    </span>
                    <span className="text-slate-500 text-[11px] block truncate mt-0.5">
                      {res.formattedAddress}
                    </span>
                  </div>
                  <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:text-orange-600 shrink-0" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Map View Container (min-h-0 cho phép flex co giãn hoàn hảo mà không đẩy footer) */}
        <div className="relative flex-1 min-h-0 w-full bg-slate-100">
          <div ref={mapContainerRef} className="w-full h-full min-h-[160px] z-0" />

          {/* Bộ chuyển đổi loại bản đồ: Đường phố rõ nét / Vệ tinh thực tế / Địa hình */}
          <div className="absolute top-3 right-3 z-10 flex items-center bg-white/95 backdrop-blur-xs rounded-xl p-1 shadow-md border border-slate-200 text-xs">
            <button
              type="button"
              onClick={() => handleSwitchMapType('streets')}
              className={`px-2.5 py-1.5 rounded-lg font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                activeMapType === 'streets'
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
              }`}
              title="Bản đồ đường phố rõ nét, đầy đủ tên đường và địa danh (Google Maps)"
            >
              <Map className="w-3.5 h-3.5" />
              <span>Đường phố</span>
            </button>
            <button
              type="button"
              onClick={() => handleSwitchMapType('satellite')}
              className={`px-2.5 py-1.5 rounded-lg font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                activeMapType === 'satellite'
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
              }`}
              title="Ảnh chụp vệ tinh thực tế nhìn rõ từng ngôi nhà và con hẻm kèm tên đường"
            >
              <Globe className="w-3.5 h-3.5" />
              <span>Vệ tinh</span>
            </button>
            <button
              type="button"
              onClick={() => handleSwitchMapType('terrain')}
              className={`px-2.5 py-1.5 rounded-lg font-semibold flex items-center gap-1.5 transition-all cursor-pointer ${
                activeMapType === 'terrain'
                  ? 'bg-orange-600 text-white shadow-2xs'
                  : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
              }`}
              title="Bản đồ địa hình giao thông"
            >
              <Layers className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Địa hình</span>
            </button>
          </div>

          {/* Loading Overlay khi đang geocoding */}
          {isGeocoding && (
            <div className="absolute top-3 left-1/2 -translate-x-1/2 z-10 bg-white/90 backdrop-blur-xs px-3.5 py-1.5 rounded-full shadow-md border border-slate-200 text-xs font-semibold text-slate-700 flex items-center gap-2">
              <Loader2 className="w-3.5 h-3.5 animate-spin text-orange-600" />
              <span>Đang lấy địa chỉ trực tiếp...</span>
            </div>
          )}

          {/* Floating Instructions Helper Badge */}
          <div className="absolute bottom-3 left-3 z-10 hidden sm:flex items-center gap-1.5 bg-white/90 backdrop-blur-xs px-3 py-1.5 rounded-xl border border-slate-200 shadow-2xs text-[11px] text-slate-600 pointer-events-none">
            <Info className="w-3.5 h-3.5 text-orange-600 shrink-0" />
            <span>Nhấp vào bất kỳ điểm nào trên bản đồ hoặc kéo ghim cam để thay đổi vị trí</span>
          </div>
        </div>

        {/* Bottom Selected Location Bar & Confirmation — LUÔN HIỂN THỊ RÕ RÀNG */}
        <div className="shrink-0 p-3 sm:p-4 border-t border-slate-200 bg-white space-y-2 z-10 shadow-xs">
          <div className="flex items-center justify-between text-xs">
            <span className="font-bold text-slate-700 flex items-center gap-1.5 uppercase tracking-wider text-[11px]">
              <MapPin className="w-3.5 h-3.5 text-orange-600" />
              Địa chỉ nhận hàng đã chọn
            </span>
            {selectedCoords && (
              <span className="text-[11px] font-mono text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md border border-slate-200/60">
                {selectedCoords.lat.toFixed(5)}, {selectedCoords.lon.toFixed(5)}
              </span>
            )}
          </div>

          {/* Input cho phép xem và điều chỉnh địa chỉ chi tiết */}
          <div className="relative">
            <input
              type="text"
              value={editableAddress}
              onChange={(e) => setEditableAddress(e.target.value)}
              placeholder="Địa chỉ sẽ tự động hiển thị khi bạn chọn vị trí trên bản đồ..."
              className="w-full px-3.5 py-2 rounded-xl text-xs sm:text-sm font-medium border border-slate-200 focus:outline-none focus:ring-2 focus:ring-orange-500/20 focus:border-orange-500 text-slate-800 bg-slate-50/60 focus:bg-white transition-all truncate"
            />
          </div>

          <div className="flex items-center justify-between gap-2 pt-0.5">
            <p className="text-[11px] text-slate-400 hidden sm:block truncate">
              * Bạn có thể bổ sung thêm số nhà, số phòng hoặc tên tòa nhà trực tiếp vào ô trên.
            </p>
            <div className="flex items-center gap-2 ml-auto shrink-0">
              <button
                type="button"
                onClick={onClose}
                className="px-3.5 sm:px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                disabled={!editableAddress.trim() && !selectedAddress.trim()}
                className="px-4 sm:px-5 py-2 bg-orange-600 hover:bg-orange-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-xs sm:text-sm rounded-xl shadow-2xs hover:shadow-xs active:scale-[0.99] transition-all flex items-center gap-1.5 cursor-pointer shrink-0"
              >
                <Check className="w-4 h-4" />
                <span>Xác nhận & Sử dụng địa chỉ này</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AddressMapModal
