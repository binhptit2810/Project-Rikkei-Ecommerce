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
 * Hàm hỗ trợ Reverse Geocoding qua OpenStreetMap Nominatim miễn phí.
 * Chuyển đổi tọa độ (lat, lng) thành chuỗi địa chỉ chi tiết tại Việt Nam.
 */
export const reverseGeocode = async (lat, lng) => {
  try {
    const url = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`
    const res = await fetch(url, {
      headers: {
        'Accept-Language': 'vi,en;q=0.9',
      },
    })
    if (!res.ok) throw new Error('Không thể lấy thông tin địa chỉ từ tọa độ')
    const data = await res.json()
    if (!data) return null

    const addr = data.address || {}
    // Xây dựng địa chỉ tự nhiên và đầy đủ theo chuẩn Việt Nam
    const parts = []
    if (addr.house_number) parts.push(`Số ${addr.house_number}`)
    if (addr.road) parts.push(addr.road)
    if (addr.suburb || addr.quarter || addr.neighbourhood) {
      parts.push(addr.suburb || addr.quarter || addr.neighbourhood)
    }
    if (addr.city_district || addr.district || addr.county) {
      parts.push(addr.city_district || addr.district || addr.county)
    }
    if (addr.city || addr.province || addr.state) {
      parts.push(addr.city || addr.province || addr.state)
    }

    const formattedAddress = parts.length >= 2 ? parts.join(', ') : data.display_name
    return {
      formattedAddress,
      raw: data,
      lat,
      lng,
    }
  } catch (err) {
    console.warn('Lỗi reverse geocode:', err)
    return null
  }
}

/**
 * Tìm kiếm địa điểm theo từ khóa qua Nominatim OpenStreetMap.
 */
export const searchLocation = async (query) => {
  if (!query || !query.trim()) return []
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
      query.trim()
    )}&countrycodes=vn&limit=5&addressdetails=1`
    const res = await fetch(url, {
      headers: {
        'Accept-Language': 'vi,en;q=0.9',
      },
    })
    if (!res.ok) return []
    const list = await res.json()
    return Array.isArray(list) ? list : []
  } catch (err) {
    console.warn('Lỗi tìm kiếm địa điểm:', err)
    return []
  }
}
