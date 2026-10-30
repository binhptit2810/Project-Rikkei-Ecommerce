import React, { useState, useRef } from 'react'
import { UploadCloud, Image as ImageIcon, Link as LinkIcon, Trash2, Loader2, AlertCircle, RefreshCw } from 'lucide-react'
import uploadService from '../../services/uploadService'

export const ImageUploadInput = ({
  value = '',
  onChange,
  label = 'Ảnh đại diện',
  required = false,
  placeholder = 'https://example.com/image.jpg',
}) => {
  const [activeTab, setActiveTab] = useState('file') // 'file' | 'url'
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef(null)

  const handleFileSelect = async (file) => {
    if (!file) return

    // Kiểm tra định dạng ảnh
    if (!file.type.startsWith('image/')) {
      setUploadError('Vui lòng chọn một tệp hình ảnh hợp lệ (PNG, JPG, WEBP, GIF).')
      return
    }

    // Kiểm tra dung lượng (tối đa 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setUploadError('Dung lượng ảnh vượt quá 5MB. Vui lòng chọn ảnh nhẹ hơn.')
      return
    }

    setUploadError(null)
    setIsUploading(true)

    try {
      const response = await uploadService.uploadImage(file)
      if (response && response.success && response.data?.url) {
        onChange(response.data.url)
      } else {
        setUploadError(response?.message || 'Tải ảnh lên thất bại')
      }
    } catch (err) {
      console.error('Lỗi khi tải ảnh lên:', err)
      const message = err.response?.data?.message || err.message || 'Không thể tải ảnh lên máy chủ'
      setUploadError(message)
    } finally {
      setIsUploading(false)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  const handleFileInputChange = (e) => {
    const file = e.target.files?.[0]
    if (file) {
      handleFileSelect(file)
    }
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
    const file = e.dataTransfer.files?.[0]
    if (file) {
      handleFileSelect(file)
    }
  }

  const handleClearImage = () => {
    onChange('')
    setUploadError(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <div className="space-y-2">
      {/* Label & Tabs */}
      <div className="flex items-center justify-between">
        <label className="block text-xs font-bold text-slate-700">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
        <div className="flex items-center bg-slate-100 p-0.5 rounded-lg border border-slate-200 text-[11px] font-semibold">
          <button
            type="button"
            onClick={() => {
              setActiveTab('file')
              setUploadError(null)
            }}
            className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'file'
                ? 'bg-white text-orange-600 shadow-2xs font-bold'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <UploadCloud className="w-3.5 h-3.5" />
            <span>Từ máy tính</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setActiveTab('url')
              setUploadError(null)
            }}
            className={`px-2.5 py-1 rounded-md transition-all flex items-center gap-1.5 cursor-pointer ${
              activeTab === 'url'
                ? 'bg-white text-orange-600 shadow-2xs font-bold'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            <LinkIcon className="w-3.5 h-3.5" />
            <span>Link URL</span>
          </button>
        </div>
      </div>

      {/* Upload Error Banner */}
      {uploadError && (
        <div className="p-2.5 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
          <span className="flex-1">{uploadError}</span>
        </div>
      )}

      {/* Tab: Tải từ máy tính */}
      {activeTab === 'file' && (
        <div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileInputChange}
            accept="image/jpeg,image/png,image/webp,image/gif,image/svg+xml,image/bmp"
            className="hidden"
          />

          {value ? (
            /* Có ảnh: hiển thị preview cùng nút thay đổi / xóa */
            <div className="relative group border border-slate-200 rounded-2xl p-3 bg-slate-50/60 flex items-center gap-3">
              <div className="w-20 h-20 rounded-xl overflow-hidden bg-white border border-slate-200/80 shrink-0 flex items-center justify-center shadow-2xs">
                <img
                  src={value}
                  alt="Ảnh xem trước"
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.currentTarget.src = 'https://placehold.co/100x100?text=Loi+Anh'
                  }}
                />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-semibold text-slate-800 truncate mb-1">
                  Đã tải ảnh lên thành công
                </p>
                <p className="text-[11px] text-slate-400 truncate mb-2" title={value}>
                  {value}
                </p>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={isUploading}
                    className="px-2.5 py-1 text-[11px] font-bold text-orange-600 bg-orange-50 hover:bg-orange-100 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <RefreshCw className="w-3 h-3" />
                    <span>Chọn ảnh khác</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleClearImage}
                    disabled={isUploading}
                    className="px-2.5 py-1 text-[11px] font-bold text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <Trash2 className="w-3 h-3" />
                    <span>Xóa</span>
                  </button>
                </div>
              </div>
            </div>
          ) : (
            /* Chưa có ảnh: Dropzone để kéo thả hoặc click chọn */
            <div
              onClick={() => !isUploading && fileInputRef.current?.click()}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              className={`border-2 border-dashed rounded-2xl p-6 text-center transition-all cursor-pointer ${
                isDragging
                  ? 'border-orange-500 bg-orange-50/60'
                  : 'border-slate-200 hover:border-orange-300 hover:bg-slate-50/60'
              } ${isUploading ? 'opacity-60 cursor-not-allowed' : ''}`}
            >
              {isUploading ? (
                <div className="flex flex-col items-center justify-center py-2">
                  <Loader2 className="w-8 h-8 text-orange-600 animate-spin mb-2" />
                  <p className="text-xs font-bold text-slate-700">Đang tải ảnh lên máy chủ...</p>
                  <p className="text-[11px] text-slate-400">Vui lòng chờ trong giây lát</p>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center">
                  <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-600 flex items-center justify-center mb-2 shadow-2xs">
                    <UploadCloud className="w-5 h-5" />
                  </div>
                  <p className="text-xs font-bold text-slate-700 mb-0.5">
                    Nhấn để chọn ảnh từ máy tính
                  </p>
                  <p className="text-[11px] text-slate-400">
                    hoặc kéo thả tệp vào đây (PNG, JPG, WEBP tối đa 5MB)
                  </p>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Tab: Nhập URL trực tiếp */}
      {activeTab === 'url' && (
        <div className="space-y-2">
          <div className="relative">
            <input
              type="url"
              value={value}
              onChange={(e) => onChange(e.target.value)}
              placeholder={placeholder}
              className="w-full px-3.5 py-2.5 rounded-xl border border-slate-200 text-xs focus:outline-none focus:ring-2 focus:ring-orange-500 pr-9"
            />
            {value && (
              <button
                type="button"
                onClick={handleClearImage}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
                title="Xóa URL"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Preview nhỏ nếu đã nhập URL */}
          {value && (
            <div className="flex items-center gap-3 p-2 bg-slate-50 border border-slate-200/80 rounded-xl">
              <div className="w-12 h-12 rounded-lg bg-white border border-slate-200 overflow-hidden shrink-0 flex items-center justify-center">
                <img
                  src={value}
                  alt="Xem trước"
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.currentTarget.src = 'https://placehold.co/60x60?text=Loi+URL'
                  }}
                />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-[11px] font-semibold text-slate-700">Xem trước ảnh từ URL</p>
                <p className="text-[10px] text-slate-400 truncate">{value}</p>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default ImageUploadInput

