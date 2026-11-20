import React, { useState, useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import {
  MapPin,
  Search,
  Navigation,
  X,
  Check,
  RotateCcw,
  AlertCircle,
  Loader2,
} from 'lucide-react'
import { reverseGeocode, searchLocation, getIpLocation } from '../../utils/addressStorage'

// Custom Pin icon dạng SVG sắc nét, không bị lỗi 404 ảnh marker trong Vite
const createPinIcon = () =>
  L.divIcon({
    className: 'leaflet-custom-marker',
    html: `
      <div style="position: relative; width: 38px; height: 38px; transform: translate(-19px, -38px); pointer-events: none;">
        <svg width="38" height="38" viewBox="0 0 24 24" fill="#ef4444" stroke="#ffffff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" style="filter: drop-shadow(0 4px 6px rgba(0,0,0,0.35));">
          <path d="M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0" />
          <circle cx="12" cy="10" r="3" fill="#ffffff" stroke="#ef4444" stroke-width="1" />
        </svg>
      </div>
    `,
    iconSize: [0, 0],
    iconAnchor: [0, 0],
  })

/**
 * Modal Bản đồ chọn vị trí & Định vị hiện tại qua OpenStreetMap (Leaflet).
 */
export const AddressMapModal = ({
  isOpen,
  onClose,
  onSelectAddress,
  initialAddress = '',
}) => {
  const mapContainerRef = useRef(null)
  const mapInstanceRef = useRef(null)
  const markerRef = useRef(null)

  // Tọa độ mặc định: Trung tâm Hà Nội (21.0285, 105.8542)
  const defaultCoords = { lat: 21.028511, lng: 105.854167 }
  const [currentCoords, setCurrentCoords] = useState(defaultCoords)
  const [resolvedAddress, setResolvedAddress] = useState(initialAddress || '')
  const [isResolving, setIsResolving] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [isSearching, setIsSearching] = useState(false)
  const [isLocating, setIsLocating] = useState(false)
  const [gpsError, setGpsError] = useState(null)

  // Khởi tạo và đồng bộ bản đồ khi Modal mở
  useEffect(() => {
    if (!isOpen) return

    // Timeout ngắn để đảm bảo DOM của modal đã mount hoàn tất
    const timer = setTimeout(() => {
      if (!mapContainerRef.current) return

      if (!mapInstanceRef.current) {
        const map = L.map(mapContainerRef.current, {
          center: [currentCoords.lat, currentCoords.lng],
          zoom: 15,
          zoomControl: true,
        })

        // Lớp bản đồ Google Maps tiếng Việt sắc nét, chuẩn xác, không bị watermark API key, CDN Google cực nhanh tại VN
        L.tileLayer('https://{s}.google.com/vt/lyrs=m&hl=vi&x={x}&y={y}&z={z}', {
          maxZoom: 20,
          subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
          attribution: '© Google Maps',
        }).addTo(map)

        // Marker có thể kéo thả
        const marker = L.marker([currentCoords.lat, currentCoords.lng], {
          icon: createPinIcon(),
          draggable: true,
        }).addTo(map)

        // Sự kiện click trên bản đồ để chuyển ghim
        map.on('click', (e) => {
          const { lat, lng } = e.latlng
          marker.setLatLng([lat, lng])
          updateLocation(lat, lng)
        })

        // Sự kiện kéo thả ghim
        marker.on('dragend', () => {
          const latLng = marker.getLatLng()
          updateLocation(latLng.lat, latLng.lng)
        })

        mapInstanceRef.current = map
        markerRef.current = marker

        // Tự động fix lỗi xám map khi hiển thị bên trong Modal
        map.invalidateSize()
      } else {
        mapInstanceRef.current.invalidateSize()
      }

      // Nếu đã có initialAddress thì gán ngay làm địa chỉ hiển thị
      if (initialAddress && initialAddress.trim().length > 3) {
        setResolvedAddress(initialAddress.trim())
        // Cố gắng tìm vị trí trên bản đồ tương ứng với địa chỉ ban đầu
        searchLocation(initialAddress.trim(), currentCoords)
          .then((results) => {
            if (results && results.length > 0) {
              const first = results[0]
              const lat = parseFloat(first.lat)
              const lng = parseFloat(first.lon || first.lng)
              if (mapInstanceRef.current && markerRef.current) {
                mapInstanceRef.current.flyTo([lat, lng], 16, { duration: 0.8 })
                markerRef.current.setLatLng([lat, lng])
              }
              setCurrentCoords({ lat, lng })
            }
          })
          .catch((e) => console.warn('Lỗi định vị vị trí ban đầu:', e))
      } else {
        // Hoặc giải mã tọa độ mặc định
        updateLocation(currentCoords.lat, currentCoords.lng)
      }
    }, 150)

    return () => {
      clearTimeout(timer)
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        markerRef.current = null
      }
    }
  }, [isOpen])

  // Cập nhật vị trí và gọi reverse geocoding để lấy địa chỉ chữ
  const updateLocation = async (lat, lng) => {
    setCurrentCoords({ lat, lng })
    setIsResolving(true)
    setGpsError(null)
    try {
      const res = await reverseGeocode(lat, lng)
      if (res && res.formattedAddress) {
        setResolvedAddress(res.formattedAddress)
      } else {
        setResolvedAddress(`Tọa độ: ${lat.toFixed(5)}, ${lng.toFixed(5)}`)
      }
    } catch (err) {
      console.warn('Lỗi phân giải địa chỉ:', err)
    } finally {
      setIsResolving(false)
    }
  }

  // Định vị vị trí hiện tại (Tự động kết hợp GPS và Dự phòng IP mạng)
  const handleGetLocation = () => {
    setIsLocating(true)
    setGpsError(null)

    const fallbackToIp = async () => {
      try {
        const ipLoc = await getIpLocation()
        if (ipLoc) {
          if (mapInstanceRef.current && markerRef.current) {
            mapInstanceRef.current.flyTo([ipLoc.lat, ipLoc.lng], 16, { duration: 0.8 })
            markerRef.current.setLatLng([ipLoc.lat, ipLoc.lng])
          }
          updateLocation(ipLoc.lat, ipLoc.lng)
        } else {
          setGpsError('Không thể xác định vị trí tự động. Bạn hãy bấm trực tiếp lên bản đồ để chọn.')
        }
      } catch (e) {
        setGpsError('Không thể xác định vị trí tự động. Bạn hãy bấm trực tiếp lên bản đồ để chọn.')
      } finally {
        setIsLocating(false)
      }
    }

    if (!navigator.geolocation) {
      fallbackToIp()
      return
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords
        setIsLocating(false)

        if (mapInstanceRef.current && markerRef.current) {
          mapInstanceRef.current.flyTo([latitude, longitude], 16, { duration: 0.8 })
          markerRef.current.setLatLng([latitude, longitude])
        }
        updateLocation(latitude, longitude)
      },
      (err) => {
        console.warn('GPS thiết bị không khả dụng, tự động chuyển sang định vị mạng IP:', err)
        fallbackToIp()
      },
      {
        enableHighAccuracy: true,
        timeout: 5000,
        maximumAge: 30000,
      }
    )
  }

  // Tìm kiếm địa điểm
  const handleSearch = async (queryText) => {
    const q = queryText !== undefined ? queryText : searchQuery
    if (!q || !q.trim()) return

    setIsSearching(true)
    setGpsError(null)
    try {
      const results = await searchLocation(q, currentCoords)
      setSearchResults(results)
      if (results.length === 0) {
        setGpsError(`Không tìm thấy kết quả nào khớp với "${q.trim()}". Vui lòng thử từ khóa khác.`)
      }
    } catch (err) {
      console.warn('Lỗi tìm kiếm:', err)
      setGpsError('Lỗi kết nối khi tìm kiếm địa điểm.')
    } finally {
      setIsSearching(false)
    }
  }

  // Chọn một kết quả tìm kiếm từ danh sách gợi ý
  const selectSearchResult = (item) => {
    const lat = parseFloat(item.lat)
    const lng = parseFloat(item.lon || item.lng)

    if (mapInstanceRef.current && markerRef.current) {
      mapInstanceRef.current.flyTo([lat, lng], 16, { duration: 0.8 })
      markerRef.current.setLatLng([lat, lng])
    }

    const addr = item.display_name || item.formattedAddress
    setResolvedAddress(addr)
    setCurrentCoords({ lat, lng })
    setSearchResults([])
    setSearchQuery(addr)
  }

  // Xác nhận vị trí đã chọn
  const handleConfirm = () => {
    if (!resolvedAddress) return
    onSelectAddress(resolvedAddress, currentCoords)
    onClose()
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-5 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="bg-white w-full max-w-3xl rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[92vh] animate-in zoom-in-95 duration-200">
        {/* Header Modal */}
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center shrink-0 border border-red-200/60 shadow-xs">
              <MapPin className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-slate-900 text-sm sm:text-base">
                Chọn địa điểm trên bản đồ
              </h3>
              <p className="text-[11px] sm:text-xs text-slate-500">
                Bấm vào bản đồ hoặc kéo ghim để chọn chính xác địa chỉ nhận hàng
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-slate-200/70 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Thanh công cụ tìm kiếm & nút Định vị hiện tại */}
        <div className="p-3 sm:p-4 border-b border-slate-100 bg-white space-y-2">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="Nhập tên đường, phường/xã, tòa nhà để tìm..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                className="w-full h-10 pl-9 pr-8 text-xs bg-slate-50 hover:bg-white focus:bg-white border border-slate-200 focus:border-red-500 rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500/20 transition-all"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 cursor-pointer"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
            </div>

            <button
              type="button"
              onClick={() => handleSearch()}
              disabled={isSearching || !searchQuery.trim()}
              className="h-10 px-4 bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold rounded-xl transition-all disabled:opacity-50 cursor-pointer flex items-center gap-1.5 shrink-0 shadow-xs"
            >
              {isSearching ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
              <span className="hidden sm:inline">Tìm kiếm</span>
            </button>

            {/* Nút Lấy vị trí GPS hiện tại */}
            <button
              type="button"
              onClick={handleGetLocation}
              disabled={isLocating}
              title="Lấy vị trí hiện tại của bạn"
              className="h-10 px-3.5 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200/80 text-xs font-bold rounded-xl transition-all cursor-pointer flex items-center gap-1.5 shrink-0 shadow-xs"
            >
              {isLocating ? (
                <Loader2 className="w-3.5 h-3.5 text-blue-600 animate-spin" />
              ) : (
                <Navigation className="w-3.5 h-3.5 text-blue-600" />
              )}
              <span className="hidden sm:inline">Vị trí hiện tại</span>
            </button>
          </div>

          {/* Gợi ý kết quả tìm kiếm */}
          {searchResults.length > 0 && (
            <div className="bg-white border border-slate-200 rounded-xl shadow-lg overflow-hidden divide-y divide-slate-100 max-h-44 overflow-y-auto">
              {searchResults.map((item, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => selectSearchResult(item)}
                  className="w-full text-left p-2.5 hover:bg-slate-50 text-xs text-slate-700 flex items-start gap-2 cursor-pointer transition-colors"
                >
                  <MapPin className="w-3.5 h-3.5 text-red-500 shrink-0 mt-0.5" />
                  <span className="line-clamp-1">{item.display_name}</span>
                </button>
              ))}
            </div>
          )}

          {gpsError && (
            <div className="p-2 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-[11px] flex items-center gap-2">
              <AlertCircle className="w-3.5 h-3.5 text-amber-600 shrink-0" />
              <span>{gpsError}</span>
            </div>
          )}
        </div>

        {/* Khung hiển thị Bản đồ Leaflet OpenStreetMap */}
        <div className="relative flex-1 min-h-[340px] sm:min-h-[420px] bg-slate-100">
          <div ref={mapContainerRef} className="w-full h-full min-h-[340px] sm:min-h-[420px] z-10" />

          {/* Hộp xem trước địa chỉ đã chọn nổi trên bản đồ */}
          <div className="absolute bottom-3 left-3 right-3 z-20 bg-white/95 backdrop-blur-sm p-3.5 rounded-2xl border border-slate-200 shadow-xl space-y-1">
            <div className="flex items-center gap-1.5 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
              <MapPin className="w-3.5 h-3.5 text-red-500" />
              <span>Địa chỉ đã chọn</span>
              {isResolving && (
                <span className="text-blue-600 flex items-center gap-1 font-normal lowercase">
                  <Loader2 className="w-3 h-3 animate-spin" /> đang xác định địa chỉ...
                </span>
              )}
            </div>
            <p className="text-xs sm:text-sm font-bold text-slate-900 line-clamp-2 leading-snug">
              {resolvedAddress || 'Chưa chọn địa điểm'}
            </p>
          </div>
        </div>

        {/* Footer Modal: Nút Hủy & Xác nhận */}
        <div className="p-3.5 sm:p-4 border-t border-slate-100 bg-slate-50/70 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 text-xs font-bold transition-colors cursor-pointer"
          >
            Hủy bỏ
          </button>

          <button
            type="button"
            onClick={handleConfirm}
            disabled={!resolvedAddress || isResolving}
            className="px-6 py-2.5 rounded-xl bg-red-500 hover:bg-red-600 disabled:opacity-50 text-white text-xs font-bold transition-all shadow-md shadow-red-500/20 cursor-pointer flex items-center gap-2"
          >
            <Check className="w-4 h-4" />
            <span>Xác nhận địa chỉ này</span>
          </button>
        </div>
      </div>
    </div>
  )
}

export default AddressMapModal
