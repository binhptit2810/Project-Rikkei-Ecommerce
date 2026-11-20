/**
 * Quản lý sổ địa chỉ giao hàng của người dùng (Saved Addresses).
 * Lưu trữ bền vững theo từng tài khoản (userId) trong localStorage.
 */

const STORAGE_PREFIX = 'rikkei_saved_addresses_'

export const getStorageKey = (userId) => {
  return `${STORAGE_PREFIX}${userId || 'guest'}`
}

export const getSavedAddresses = (userId) => {
  try {
    const raw = localStorage.getItem(getStorageKey(userId))
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (err) {
    console.warn('Lỗi đọc sổ địa chỉ từ localStorage:', err)
    return []
  }
}

export const saveAddress = (userId, addressData) => {
  try {
    const current = getSavedAddresses(userId)
    const newAddress = {
      id: addressData.id || `addr_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      recipientName: addressData.recipientName?.trim() || '',
      recipientPhone: addressData.recipientPhone?.trim() || '',
      shippingAddress: addressData.shippingAddress?.trim() || '',
      label: addressData.label?.trim() || 'Địa chỉ giao hàng',
      isDefault: addressData.isDefault || current.length === 0,
      createdAt: addressData.createdAt || new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    // Nếu địa chỉ này được đánh dấu là mặc định, bỏ mặc định ở các địa chỉ khác
    let updatedList
    const existingIndex = current.findIndex((item) => item.id === newAddress.id)

    if (newAddress.isDefault) {
      current.forEach((item) => {
        item.isDefault = false
      })
    }

    if (existingIndex >= 0) {
      current[existingIndex] = newAddress
      updatedList = [...current]
    } else {
      updatedList = [newAddress, ...current]
    }

    localStorage.setItem(getStorageKey(userId), JSON.stringify(updatedList))
    return { success: true, data: newAddress, list: updatedList }
  } catch (err) {
    console.error('Lỗi lưu địa chỉ vào localStorage:', err)
    return { success: false, error: err.message }
  }
}

export const deleteAddress = (userId, addressId) => {
  try {
    const current = getSavedAddresses(userId)
    const filtered = current.filter((item) => item.id !== addressId)
    // Nếu xóa mất địa chỉ mặc định mà danh sách vẫn còn, đặt phần tử đầu tiên làm mặc định
    if (filtered.length > 0 && !filtered.some((item) => item.isDefault)) {
      filtered[0].isDefault = true
    }
    localStorage.setItem(getStorageKey(userId), JSON.stringify(filtered))
    return { success: true, list: filtered }
  } catch (err) {
    console.error('Lỗi xóa địa chỉ khỏi localStorage:', err)
    return { success: false, error: err.message }
  }
}

export const setDefaultAddress = (userId, addressId) => {
  try {
    const current = getSavedAddresses(userId)
    current.forEach((item) => {
      item.isDefault = item.id === addressId
    })
    localStorage.setItem(getStorageKey(userId), JSON.stringify(current))
    return { success: true, list: current }
  } catch (err) {
    console.error('Lỗi đặt địa chỉ mặc định:', err)
    return { success: false, error: err.message }
  }
}

/**
 * Chuẩn hóa chuỗi địa chỉ tiếng Việt từ kết quả Photon (Komoot OSM).
 */
export const buildPhotonAddress = (props) => {
  if (!props) return ''
  const parts = []

  // Tên địa điểm cụ thể (nếu có và không trùng với tên đường)
  if (props.name && props.name !== props.street && !props.name.startsWith(props.street || '___')) {
    parts.push(props.name)
  }

  // Số nhà + Tên đường
  if (props.housenumber && props.street) {
    parts.push(`Số ${props.housenumber} ${props.street}`)
  } else if (props.street) {
    parts.push(props.street)
  }

  // Phường / Xã / Khu dân cư / Tổ dân phố (locality)
  if (props.locality && props.locality !== props.name) {
    parts.push(props.locality)
  }

  // Quận / Huyện (district)
  if (props.district) {
    parts.push(props.district)
  }

  // Tỉnh / Thành phố (city hoặc state)
  if (props.city) {
    parts.push(props.city)
  } else if (props.state && props.state !== props.city) {
    parts.push(props.state)
  }

  // Quốc gia (nếu chuỗi địa chỉ quá ngắn)
  if (parts.length <= 1 && props.country) {
    parts.push(props.country)
  }

  // Lọc các phần tử trùng lặp
  const uniqueParts = parts.filter((p, idx) => Boolean(p) && parts.indexOf(p) === idx)
  return uniqueParts.join(', ')
}

/**
 * Chuẩn hóa chuỗi địa chỉ tiếng Việt từ BigDataCloud.
 */
export const buildBigDataCloudAddress = (data) => {
  if (!data) return ''
  const parts = []
  if (data.locality) parts.push(data.locality)
  if (data.city && data.city !== data.locality) parts.push(data.city)
  if (data.principalSubdivision && data.principalSubdivision !== data.city) {
    parts.push(data.principalSubdivision)
  }
  if (parts.length === 0 && data.countryName) parts.push(data.countryName)
  return parts.filter(Boolean).join(', ')
}

/**
 * Tự động lấy vị trí xấp xỉ qua IP mạng khi GPS không khả dụng hoặc bị từ chối quyền.
 */
export const getIpLocation = async () => {
  try {
    const res = await fetch('https://api.bigdatacloud.net/data/reverse-geocode-client?localityLanguage=vi')
    if (res.ok) {
      const data = await res.json()
      if (data && data.latitude && data.longitude) {
        const formattedAddress = buildBigDataCloudAddress(data)
        return {
          lat: data.latitude,
          lng: data.longitude,
          formattedAddress: formattedAddress || data.city || data.principalSubdivision || 'Vị trí hiện tại',
          raw: data,
        }
      }
    }
  } catch (e) {
    console.warn('Lỗi định vị qua IP:', e)
  }
  return null
}

/**
 * Hàm hỗ trợ Reverse Geocoding chuyển đổi tọa độ (lat, lng) thành chuỗi địa chỉ tiếng Việt.
 * Áp dụng cơ chế Multi-Provider Fallback:
 * 1. Photon (Komoot OpenStreetMap) - Nhanh, không bị lỗi DNS tại Việt Nam, hỗ trợ số nhà, đường, phường, quận.
 * 2. BigDataCloud Reverse Client - Dự phòng quốc tế miễn phí, có CORS, dịch sang tiếng Việt.
 */
export const reverseGeocode = async (lat, lng) => {
  if (typeof lat !== 'number' || typeof lng !== 'number') return null

  // 1. Thử qua dịch vụ Photon Komoot Reverse
  try {
    const url = `https://photon.komoot.io/reverse?lat=${lat}&lon=${lng}`
    const res = await fetch(url)
    if (res.ok) {
      const data = await res.json()
      if (data && data.features && data.features.length > 0) {
        const feature = data.features[0]
        const formattedAddress = buildPhotonAddress(feature.properties)
        if (formattedAddress && formattedAddress.trim()) {
          return {
            formattedAddress,
            raw: feature,
            lat,
            lng,
          }
        }
      }
    }
  } catch (err) {
    console.warn('Lỗi reverse geocode với Photon, chuyển sang BigDataCloud:', err)
  }

  // 2. Dự phòng qua dịch vụ BigDataCloud Client API
  try {
    const url = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=vi`
    const res = await fetch(url)
    if (res.ok) {
      const data = await res.json()
      const formattedAddress = buildBigDataCloudAddress(data)
      if (formattedAddress && formattedAddress.trim()) {
        return {
          formattedAddress,
          raw: data,
          lat,
          lng,
        }
      }
    }
  } catch (err) {
    console.warn('Lỗi reverse geocode với BigDataCloud:', err)
  }

  // Fallback tọa độ nếu mọi dịch vụ gặp sự cố mạng
  return {
    formattedAddress: `Tọa độ: ${lat.toFixed(5)}, ${lng.toFixed(5)}`,
    raw: null,
    lat,
    lng,
  }
}

/**
 * Tìm kiếm địa điểm theo từ khóa (Geocoding / Search Autocomplete).
 * Sử dụng Photon Komoot API - Tích hợp đầy đủ dữ liệu OpenStreetMap,
 * hỗ trợ tìm kiếm không dấu / có dấu tiếng Việt, không bị DNS block tại VN.
 */
export const searchLocation = async (query, centerCoords = null) => {
  if (!query || !query.trim()) return []

  try {
    const trimmed = query.trim()
    const lat = centerCoords?.lat || 21.0285
    const lon = centerCoords?.lng || centerCoords?.lon || 105.8542
    // Thêm lat/lon bias để ưu tiên kết quả tìm kiếm gần vị trí người dùng ở Việt Nam
    const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(trimmed)}&lat=${lat}&lon=${lon}&limit=8`

    const res = await fetch(url)
    if (!res.ok) return []
    const data = await res.json()

    if (!data || !data.features || !Array.isArray(data.features)) return []

    return data.features.map((feature) => {
      const coords = feature.geometry?.coordinates || [105.8542, 21.0285]
      const lon = coords[0]
      const lat = coords[1]
      const formatted = buildPhotonAddress(feature.properties)
      const label = formatted || feature.properties?.name || trimmed

      return {
        lat,
        lon,
        lng: lon,
        display_name: label,
        formattedAddress: label,
        properties: feature.properties,
        raw: feature,
      }
    })
  } catch (err) {
    console.warn('Lỗi tìm kiếm địa điểm với Photon:', err)
    return []
  }
}
