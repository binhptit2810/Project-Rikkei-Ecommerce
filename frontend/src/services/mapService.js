/**
 * Service xử lý tìm kiếm địa chỉ, geocoding và reverse geocoding
 * Sử dụng OpenStreetMap / Photon / BigDataCloud (Miễn phí, không yêu cầu thẻ hay API key)
 */

export const PRESET_LOCATIONS = [
  { name: 'Hà Nội', lat: 21.028511, lon: 105.854444 },
  { name: 'TP. Hồ Chí Minh', lat: 10.776889, lon: 106.700806 },
  { name: 'Đà Nẵng', lat: 16.054407, lon: 108.202167 },
  { name: 'Hải Phòng', lat: 20.844912, lon: 106.688084 },
  { name: 'Cần Thơ', lat: 10.045162, lon: 105.746857 },
]

export const DEFAULT_CENTER = {
  lat: 21.028511,
  lon: 105.854444,
  zoom: 14,
}

/**
 * Định dạng địa chỉ tiếng Việt rõ ràng, mạch lạc, tránh lặp từ
 */
export const formatLocationAddress = (props = {}, bdcData = null) => {
  const parts = []

  const name = props.name?.trim() || ''
  const house = props.housenumber?.trim() || ''
  const street = props.street?.trim() || ''

  let addressHead = ''
  if (house && street) {
    addressHead = `${house} ${street}`
    if (name && name !== house && name !== street && !street.toLowerCase().includes(name.toLowerCase())) {
      addressHead = `${name}, ${addressHead}`
    }
  } else if (street) {
    addressHead = street
    if (name && name !== street && !street.toLowerCase().includes(name.toLowerCase())) {
      addressHead = `${name}, ${street}`
    }
  } else if (name) {
    addressHead = name
  }

  if (addressHead) {
    parts.push(addressHead)
  }

  // Phường / Xã / Quận / Huyện
  const district = props.district?.trim() || bdcData?.locality?.trim() || ''
  if (district && !parts.some((p) => p.toLowerCase().includes(district.toLowerCase()))) {
    parts.push(district)
  }

  // Tỉnh / Thành phố
  const city =
    props.city?.trim() ||
    props.state?.trim() ||
    bdcData?.city?.trim() ||
    bdcData?.principalSubdivision?.trim() ||
    ''
  if (city && !parts.some((p) => p.toLowerCase().includes(city.toLowerCase()))) {
    parts.push(city)
  }

  // Quốc gia
  const country = props.country?.trim() || bdcData?.countryName?.trim() || 'Việt Nam'
  if (country && !parts.some((p) => p.toLowerCase().includes(country.toLowerCase()))) {
    parts.push(country)
  }

  return parts.join(', ')
}

/**
 * Tìm kiếm vị trí theo từ khóa địa chỉ
 * Ưu tiên địa điểm ở Việt Nam và lân cận vị trí hiện tại
 */
export const searchAddress = async (query, currentLat = 16.0, currentLon = 107.5) => {
  if (!query || !query.trim()) return []
  const q = query.trim()

  try {
    const latParam = currentLat ? `&lat=${currentLat}&lon=${currentLon}` : ''
    const url = `https://photon.komoot.io/api/?q=${encodeURIComponent(q)}${latParam}&limit=8`
    const res = await fetch(url)

    if (res.ok) {
      const data = await res.json()
      if (data.features && data.features.length > 0) {
        const results = data.features.map((f, idx) => {
          const p = f.properties || {}
          const coords = f.geometry?.coordinates || [0, 0]
          const formatted = formatLocationAddress(p)

          return {
            id: p.osm_id || `${idx}-${coords[0]}`,
            name: p.name || formatted || q,
            formattedAddress: formatted || p.name || q,
            lat: coords[1],
            lon: coords[0],
            countrycode: p.countrycode || '',
            properties: p,
          }
        })

        // Ưu tiên kết quả thuộc Việt Nam lên đầu
        results.sort((a, b) => {
          const aIsVn = a.countrycode === 'VN' || a.formattedAddress.includes('Việt Nam') ? 1 : 0
          const bIsVn = b.countrycode === 'VN' || b.formattedAddress.includes('Việt Nam') ? 1 : 0
          return bIsVn - aIsVn
        })

        return results
      }
    }
  } catch (err) {
    console.warn('Photon geocoding error:', err)
  }

  // Fallback: Open-Meteo Geocoding
  try {
    const omUrl = `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(
      q
    )}&count=6&language=vi`
    const omRes = await fetch(omUrl)

    if (omRes.ok) {
      const omData = await omRes.json()
      if (omData.results && omData.results.length > 0) {
        return omData.results.map((r) => {
          const parts = [r.name, r.admin3, r.admin2, r.admin1, r.country].filter(Boolean)
          const uniqueParts = parts.filter((item, index) => parts.indexOf(item) === index)
          return {
            id: r.id,
            name: r.name,
            formattedAddress: uniqueParts.join(', '),
            lat: r.latitude,
            lon: r.longitude,
            countrycode: r.country_code || '',
            properties: r,
          }
        })
      }
    }
  } catch (err) {
    console.warn('Open-Meteo geocoding error:', err)
  }

  return []
}

/**
 * Lấy địa chỉ cụ thể từ tọa độ lat, lon (Reverse Geocoding)
 */
export const reverseGeocode = async (lat, lon) => {
  let photonProps = null
  let bdcData = null

  // 1. Gọi Photon Reverse Geocoding
  try {
    const pUrl = `https://photon.komoot.io/reverse?lat=${lat}&lon=${lon}`
    const pRes = await fetch(pUrl)
    if (pRes.ok) {
      const pData = await pRes.json()
      if (pData.features && pData.features.length > 0) {
        photonProps = pData.features[0].properties
      }
    }
  } catch (err) {
    console.warn('Photon reverse geocoding failed:', err)
  }

  // 2. Gọi BigDataCloud Reverse Geocode (hỗ trợ phân cấp hành chính tiếng Việt cực tốt)
  try {
    const bUrl = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=vi`
    const bRes = await fetch(bUrl)
    if (bRes.ok) {
      bdcData = await bRes.json()
    }
  } catch (err) {
    console.warn('BigDataCloud reverse geocode failed:', err)
  }

  // Tổng hợp thông tin địa chỉ hoàn chỉnh nhất
  const formattedAddress = formatLocationAddress(photonProps, bdcData)

  return {
    lat,
    lon,
    formattedAddress:
      formattedAddress || `Vị trí tại tọa độ [${lat.toFixed(5)}, ${lon.toFixed(5)}]`,
    details: {
      photon: photonProps,
      bdc: bdcData,
    },
  }
}

export default {
  PRESET_LOCATIONS,
  DEFAULT_CENTER,
  formatLocationAddress,
  searchAddress,
  reverseGeocode,
}
