import React, { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import {
  Banknote,
  ShieldCheck,
  ShoppingBag,
  AlertCircle,
  User,
  Phone,
  MapPin,
  FileText,
  Lock,
  Ticket,
  ChevronRight,
  X,
  QrCode,
  Map,
  Navigation,
  BookOpen,
  Loader2,
} from 'lucide-react'
import { useCartStore } from '../../store/useCartStore'
import { useAuthStore } from '../../store/useAuthStore'
import { useToastStore } from '../../store/useToastStore'
import orderService from '../../services/orderService'
import LoadingSpinner from '../../components/common/LoadingSpinner'
import VoucherPickerModal from '../../components/checkout/VoucherPickerModal'
import AddressMapModal from '../../components/common/AddressMapModal'
import SavedAddressModal from '../../components/checkout/SavedAddressModal'
import {
  reverseGeocode,
  saveAddress,
  getSavedAddresses,
} from '../../utils/addressStorage'

export const CheckoutPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { items, getTotalPrice, getTotalCount, clearCart } = useCartStore()
  const { user, isAuthenticated } = useAuthStore()

  // Phân biệt luồng Mua ngay (Buy Now) và luồng Giỏ hàng (Cart Checkout)
  const buyNowItem = location.state?.buyNowItem || null
  const isBuyNow = Boolean(buyNowItem)
  const checkoutItems = isBuyNow ? [buyNowItem] : items

  const [formData, setFormData] = useState({
    recipientName: '',
    recipientPhone: '',
    shippingAddress: '',
    paymentMethod: 'COD',
    note: '',
  })

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)

  // Mã giảm giá / Voucher
  const [appliedCoupon, setAppliedCoupon] = useState(null)
  const [couponError, setCouponError] = useState(null)
  const [isVoucherModalOpen, setIsVoucherModalOpen] = useState(false)

  // Quản lý Bản đồ (Map Picker) và Sổ địa chỉ (Saved Addresses)
  const [isMapModalOpen, setIsMapModalOpen] = useState(false)
  const [isSavedAddressModalOpen, setIsSavedAddressModalOpen] = useState(false)
  const [saveForLater, setSaveForLater] = useState(true)
  const [isLocatingGps, setIsLocatingGps] = useState(false)
  const [savedCount, setSavedCount] = useState(0)

  // Tự động điền thông tin có sẵn từ tài khoản hoặc địa chỉ mặc định trong sổ địa chỉ
  useEffect(() => {
    if (user) {
      const savedList = getSavedAddresses(user.id)
      setSavedCount(savedList.length)
      const defaultSaved = savedList.find((a) => a.isDefault) || savedList[0]

      setFormData((prev) => ({
        ...prev,
        recipientName: prev.recipientName || defaultSaved?.recipientName || user.fullName || '',
        recipientPhone: prev.recipientPhone || defaultSaved?.recipientPhone || user.phone || '',
        shippingAddress: prev.shippingAddress || defaultSaved?.shippingAddress || user.address || '',
      }))
    }
  }, [user])

  // Kiểm tra quyền: Tuyệt đối không cho phép Quản trị viên vào trang đặt hàng
  useEffect(() => {
    if (user?.role === 'ADMIN') {
      navigate('/admin/dashboard', { replace: true })
      return
    }
    if (!isSubmitting) {
      if (isBuyNow && !buyNowItem) {
        navigate('/')
      } else if (!isBuyNow && items.length === 0) {
        navigate('/cart')
      }
    }
  }, [user, items, isSubmitting, isBuyNow, buyNowItem, navigate])

  const formatPrice = (price) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0)
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
    if (errorMessage) setErrorMessage(null)
  }

  const handleRemoveCoupon = () => {
    setAppliedCoupon(null)
    setCouponError(null)
  }

  // Xử lý chọn địa chỉ từ Bản đồ tương tác
  const handleSelectMapAddress = (addressStr) => {
    setFormData((prev) => ({
      ...prev,
      shippingAddress: addressStr,
    }))
    setIsMapModalOpen(false)
    useToastStore.getState().showToast({
      type: 'success',
      title: 'Đã chọn vị trí trên bản đồ',
      message: addressStr,
      duration: 3000,
    })
  }

  // Xử lý chọn từ Sổ địa chỉ đã lưu
  const handleSelectSavedAddress = (saved) => {
    if (!saved) return
    setFormData((prev) => ({
      ...prev,
      recipientName: saved.recipientName || prev.recipientName,
      recipientPhone: saved.recipientPhone || prev.recipientPhone,
      shippingAddress: saved.shippingAddress || prev.shippingAddress,
    }))
    setIsSavedAddressModalOpen(false)
    useToastStore.getState().showToast({
      type: 'success',
      title: 'Đã áp dụng địa chỉ',
      message: `${saved.recipientName} - ${saved.shippingAddress}`,
      duration: 3000,
    })
  }

  // Xử lý tự động lấy vị trí hiện tại bằng định vị GPS của trình duyệt
  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) {
      useToastStore.getState().showToast({
        type: 'error',
        title: 'Không hỗ trợ GPS',
        message: 'Trình duyệt của bạn không hỗ trợ chức năng định vị GPS.',
        duration: 3500,
      })
      return
    }

    setIsLocatingGps(true)
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const { latitude, longitude } = pos.coords
          const res = await reverseGeocode(latitude, longitude)
          if (res && res.formattedAddress) {
            setFormData((prev) => ({
              ...prev,
              shippingAddress: res.formattedAddress,
            }))
            useToastStore.getState().showToast({
              type: 'success',
              title: 'Đã xác định vị trí hiện tại',
              message: res.formattedAddress,
              duration: 3500,
            })
          } else {
            useToastStore.getState().showToast({
              type: 'error',
              title: 'Không tìm thấy địa chỉ',
              message: 'Không thể giải mã địa chỉ từ tọa độ GPS.',
              duration: 3500,
            })
          }
        } catch (err) {
          useToastStore.getState().showToast({
            type: 'error',
            title: 'Lỗi định vị',
            message: err.message || 'Lỗi khi lấy thông tin địa chỉ GPS.',
            duration: 3500,
          })
        } finally {
          setIsLocatingGps(false)
        }
      },
      (err) => {
        setIsLocatingGps(false)
        let msg = 'Không thể lấy vị trí hiện tại. Vui lòng cho phép quyền truy cập vị trí trên trình duyệt.'
        if (err.code === 1) {
          msg = 'Bạn đã từ chối quyền truy cập vị trí. Hãy bật cho phép quyền trong cài đặt trình duyệt.'
        }
        useToastStore.getState().showToast({
          type: 'error',
          title: 'Lỗi truy cập vị trí',
          message: msg,
          duration: 4000,
        })
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    )
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Chống submit trùng lặp khi đang xử lý
    if (isSubmitting) return

    // Chặn Quản trị viên đặt hàng
    if (user?.role === 'ADMIN') {
      setErrorMessage('Tài khoản Quản trị viên (ADMIN) không được phép đặt hàng.')
      return
    }

    // Kiểm tra phiên đăng nhập
    if (!isAuthenticated && !user) {
      setErrorMessage('Phiên đăng nhập đã hết hạn hoặc bạn chưa đăng nhập. Vui lòng đăng nhập lại.')
      navigate('/login', { state: { from: '/checkout' } })
      return
    }

    // Kiểm tra danh sách mặt hàng
    if (!checkoutItems || checkoutItems.length === 0) {
      setErrorMessage(
        isBuyNow
          ? 'Không tìm thấy thông tin sản phẩm mua ngay. Vui lòng chọn lại sản phẩm.'
          : 'Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán.'
      )
      navigate(isBuyNow ? '/' : '/cart')
      return
    }

    // Validation form
    if (!formData.recipientName.trim()) {
      setErrorMessage('Vui lòng nhập họ tên người nhận hàng')
      return
    }

    if (formData.recipientName.trim().length > 100) {
      setErrorMessage('Họ tên người nhận không được vượt quá 100 ký tự')
      return
    }

    const phoneRegex = /^(0|\+84)[0-9]{9}$/
    if (!formData.recipientPhone.trim()) {
      setErrorMessage('Vui lòng nhập số điện thoại người nhận hàng')
      return
    }

    if (!phoneRegex.test(formData.recipientPhone.trim())) {
      setErrorMessage('Số điện thoại không đúng định dạng (yêu cầu 10 chữ số, ví dụ: 0912345678 hoặc +84912345678)')
      return
    }

    if (!formData.shippingAddress.trim()) {
      setErrorMessage('Vui lòng nhập địa chỉ nhận hàng chi tiết')
      return
    }

    if (formData.shippingAddress.trim().length < 5) {
      setErrorMessage('Địa chỉ nhận hàng quá ngắn, vui lòng nhập chi tiết số nhà, tên đường, phường/xã')
      return
    }

    if (!formData.paymentMethod) {
      setErrorMessage('Vui lòng chọn phương thức thanh toán (COD hoặc chuyển khoản VietQR SePay)')
      return
    }

    // Validate số lượng từng sản phẩm
    for (const item of checkoutItems) {
      if (!item.quantity || item.quantity < 1) {
        setErrorMessage(`Số lượng đặt của sản phẩm "${item.name}" không hợp lệ (phải >= 1)`)
        return
      }
      if (item.stockQuantity !== undefined && item.stockQuantity !== null && item.quantity > item.stockQuantity) {
        setErrorMessage(
          `Sản phẩm "${item.name}" chỉ còn ${item.stockQuantity} món trong kho (bạn đang chọn ${item.quantity}). Vui lòng điều chỉnh lại.`
        )
        return
      }
    }

    // Chuẩn hóa DTO CreateOrderRequest theo đúng cấu trúc Backend
    const orderPayload = {
      recipientName: formData.recipientName.trim(),
      recipientPhone: formData.recipientPhone.trim(),
      shippingAddress: formData.shippingAddress.trim(),
      paymentMethod: formData.paymentMethod,
      note: formData.note.trim() || null,
      couponCode: appliedCoupon?.code || null,
      items: checkoutItems.map((item) => ({
        productId: Number(item.id),
        variantId: item.variantId ? Number(item.variantId) : null,
        selectedSize: item.selectedSize ? String(item.selectedSize).trim() : null,
        quantity: Number(item.quantity),
      })),
    }

    setErrorMessage(null)
    setIsSubmitting(true)

    try {
      // Gửi request API thật lên backend: POST /api/v1/orders
      const response = await orderService.createOrder(orderPayload)
      const createdOrder = response?.data || response

      // Kiểm tra phản hồi hợp lệ từ backend có ID đơn hàng
      if (!createdOrder || !createdOrder.id) {
        throw new Error('Dữ liệu đơn hàng phản hồi từ máy chủ không hợp lệ.')
      }

      // Tự động lưu thông tin nhận hàng vào sổ địa chỉ nếu người dùng tích chọn
      if (saveForLater && formData.shippingAddress.trim()) {
        try {
          saveAddress(user?.id, {
            recipientName: formData.recipientName.trim(),
            recipientPhone: formData.recipientPhone.trim(),
            shippingAddress: formData.shippingAddress.trim(),
            label: 'Địa chỉ nhận hàng',
          })
          setSavedCount(getSavedAddresses(user?.id).length)
        } catch (saveErr) {
          console.warn('Không thể tự động lưu sổ địa chỉ:', saveErr)
        }
      }

      // Xử lý theo phương thức thanh toán
      if (formData.paymentMethod === 'SEPAY') {
        if (!isBuyNow) {
          clearCart()
        }

        useToastStore.getState().showToast({
          type: 'info',
          title: 'Khởi tạo đơn hàng thành công',
          message: `Mã đơn #${createdOrder.orderCode || createdOrder.id}. Vui lòng quét mã VietQR MBBank để chuyển khoản hoàn tất đơn hàng.`,
          link: { to: `/orders/${createdOrder.id}`, label: 'Xem đơn hàng →' },
          duration: 3000,
        })

        navigate(`/orders/${createdOrder.id}`, {
          state: {
            orderSuccess: false,
            waitingPayment: true,
            justCreated: true,
            orderCode: createdOrder.orderCode,
            totalAmount: createdOrder.totalAmount,
          },
          replace: true,
        })
        return
      }

      // Luồng COD: Chỉ xóa giỏ hàng khi đặt hàng từ giỏ; KHÔNG xóa giỏ khi Mua ngay (Buy Now)
      if (!isBuyNow) {
        clearCart()
      }

      // Kích hoạt ô thông báo bên phải: Đặt hàng COD thành công
      useToastStore.getState().showToast({
        type: 'success',
        title: isBuyNow ? 'Mua ngay thành công!' : 'Đặt hàng thành công!',
        message: `Mã đơn hàng: #${createdOrder.orderCode || createdOrder.id} • Bạn sẽ thanh toán tiền mặt khi nhận hàng (COD)!`,
        link: { to: `/orders/${createdOrder.id}`, label: 'Xem đơn hàng →' },
        duration: 2500,
      })

      // Chuyển hướng sang trang chi tiết đơn hàng kèm state chúc mừng và mã đơn ORD-...
      navigate(`/orders/${createdOrder.id}`, {
        state: {
          orderSuccess: true,
          waitingPayment: false,
          justCreated: true,
          orderCode: createdOrder.orderCode,
          totalAmount: createdOrder.totalAmount,
        },
        replace: true,
      })
    } catch (err) {
      // Giữ nguyên giỏ hàng khi thất bại, phân tích chi tiết mã lỗi từ Backend
      let message = 'Không thể tạo đơn hàng. Vui lòng thử lại.'

      if (err.response) {
        const { status, data } = err.response
        if (status === 401) {
          message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
          navigate('/login', { state: { from: '/checkout' } })
        } else if (status === 403) {
          message = 'Bạn không có quyền thực hiện đặt hàng với tài khoản này.'
        } else if (status === 400) {
          if (data?.data && typeof data.data === 'object' && !Array.isArray(data.data)) {
            const fieldErrors = Object.values(data.data).join('; ')
            message = fieldErrors || data?.message || 'Dữ liệu đơn hàng không hợp lệ.'
          } else {
            message = data?.message || 'Thông tin đơn hàng không hợp lệ. Vui lòng kiểm tra lại.'
          }
        } else if (status === 404) {
          message = data?.message || 'Một hoặc nhiều sản phẩm trong giỏ hàng không còn tồn tại.'
        } else if (status === 409) {
          message = data?.message || 'Xung đột trạng thái đơn hàng hoặc kho hàng. Vui lòng thử lại.'
        } else if (status === 500) {
          message = data?.message || 'Lỗi hệ thống máy chủ khi xử lý đơn hàng. Vui lòng thử lại sau.'
        } else {
          message = data?.message || `Lỗi yêu cầu (HTTP ${status}). Vui lòng thử lại.`
        }
      } else if (err.request) {
        message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối mạng của bạn.'
      } else if (err.message) {
        message = err.message
      }

      setErrorMessage(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const SHIPPING_FEE = 16000
  const totalPrice = isBuyNow
    ? (Number(buyNowItem?.price) || 0) * (Number(buyNowItem?.quantity) || 1)
    : getTotalPrice()
  const totalCount = isBuyNow
    ? (Number(buyNowItem?.quantity) || 1)
    : getTotalCount()
  const discountAmount = appliedCoupon?.discountAmount || 0
  const finalPrice = Math.max(0, totalPrice - discountAmount) + SHIPPING_FEE

  if (checkoutItems.length === 0) {
    return null
  }

  return (
    <div className="min-h-[calc(100vh-140px)] bg-slate-50/60 py-10 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        {/* Breadcrumb */}
        <nav className="flex items-center gap-2 text-xs text-slate-500 mb-8">
          <Link to="/" className="hover:text-blue-600 transition-colors">
            Trang chủ
          </Link>
          <span>/</span>
          {isBuyNow ? (
            <Link to={`/products/${buyNowItem?.id || ''}`} className="hover:text-blue-600 transition-colors">
              Chi tiết sản phẩm
            </Link>
          ) : (
            <Link to="/cart" className="hover:text-blue-600 transition-colors">
              Giỏ hàng
            </Link>
          )}
          <span>/</span>
          <span className="font-bold text-slate-900">{isBuyNow ? 'Mua ngay' : 'Thanh toán'}</span>
        </nav>

        {/* Header */}
        <div className="mb-8 pb-5 border-b border-slate-200">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 border border-blue-200/80 text-blue-700 text-xs font-bold uppercase tracking-wider mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>{isBuyNow ? 'MUA NGAY (BUY NOW)' : 'SECURE CHECKOUT'}</span>
          </div>
          <h1 className="text-3xl font-extrabold text-slate-900 tracking-tight">
            {isBuyNow ? 'Xác nhận Mua ngay sản phẩm' : 'Xác nhận thông tin & Đặt hàng'}
          </h1>
          <p className="text-slate-500 text-sm mt-1">
            {isBuyNow
              ? 'Thanh toán trực tiếp cho sản phẩm bạn vừa chọn mà không làm thay đổi các sản phẩm trong giỏ hàng.'
              : 'Kiểm tra thông tin giao nhận và chọn phương thức thanh toán trước khi xác nhận.'}
          </p>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-800 rounded-2xl flex items-center justify-between text-xs sm:text-sm shadow-xs animate-in fade-in duration-200">
            <div className="flex items-center gap-2.5">
              <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
              <span className="font-semibold">{errorMessage}</span>
            </div>
            <button
              onClick={() => setErrorMessage(null)}
              className="text-red-700 hover:text-red-950 font-bold text-xs px-3 py-1 rounded-full hover:bg-red-100 cursor-pointer"
            >
              Đóng
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left Column: Recipient info & Payment methods (7/12) */}
            <div className="lg:col-span-7 space-y-6">

              {/* Card thông tin giao hàng */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 space-y-5">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100 flex-wrap gap-2">
                  <div className="flex items-center gap-3">
                    <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                      1
                    </span>
                    <h2 className="text-base font-bold text-slate-900">Thông tin người nhận</h2>
                  </div>

                  {/* Nút Chọn từ sổ địa chỉ đã lưu */}
                  <button
                    type="button"
                    onClick={() => setIsSavedAddressModalOpen(true)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-blue-200 bg-blue-50/70 hover:bg-blue-100 text-blue-700 text-xs font-bold transition-all cursor-pointer shadow-2xs hover:shadow-xs"
                    title="Chọn nhanh từ các địa chỉ bạn đã lưu"
                  >
                    <BookOpen className="w-3.5 h-3.5 text-blue-600" />
                    <span>Chọn từ sổ địa chỉ</span>
                    {savedCount > 0 && (
                      <span className="px-1.5 py-0.2 rounded-full bg-blue-600 text-white text-[10px] font-extrabold">
                        {savedCount}
                      </span>
                    )}
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-900 block">
                      Họ và tên người nhận <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        name="recipientName"
                        value={formData.recipientName}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        placeholder="Nguyễn Văn A"
                        required
                        className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-xs font-bold text-slate-900 block">
                      Số điện thoại <span className="text-red-500">*</span>
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="tel"
                        name="recipientPhone"
                        value={formData.recipientPhone}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        placeholder="0912345678"
                        required
                        className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                      />
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <label className="text-xs font-bold text-slate-900 block">
                      Địa chỉ nhận hàng chi tiết <span className="text-red-500">*</span>
                    </label>

                    <div className="flex items-center gap-2">
                      {/* Nút định vị GPS vị trí hiện tại */}
                      <button
                        type="button"
                        onClick={handleGetCurrentLocation}
                        disabled={isLocatingGps || isSubmitting}
                        className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-700 hover:text-emerald-800 bg-emerald-50 hover:bg-emerald-100 px-3 py-1.5 rounded-xl transition-all cursor-pointer border border-emerald-200/90 shadow-2xs hover:shadow-xs disabled:opacity-50"
                        title="Tự động lấy vị trí hiện tại của bạn qua GPS"
                      >
                        {isLocatingGps ? (
                          <>
                            <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-600" />
                            <span>Đang định vị...</span>
                          </>
                        ) : (
                          <>
                            <Navigation className="w-3.5 h-3.5 text-emerald-600" />
                            <span>Vị trí hiện tại</span>
                          </>
                        )}
                      </button>

                      {/* Nút mở bản đồ tương tác Leaflet */}
                      <button
                        type="button"
                        onClick={() => setIsMapModalOpen(true)}
                        disabled={isSubmitting}
                        className="inline-flex items-center gap-1.5 text-xs font-bold text-blue-700 hover:text-blue-800 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-xl transition-all cursor-pointer border border-blue-200/90 shadow-2xs hover:shadow-xs"
                        title="Mở bản đồ trực quan để chấm điểm giao hàng"
                      >
                        <Map className="w-3.5 h-3.5 text-blue-600" />
                        <span>Mở bản đồ</span>
                      </button>
                    </div>
                  </div>

                  <div className="relative">
                    <MapPin className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                    <textarea
                      name="shippingAddress"
                      value={formData.shippingAddress}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      rows="2"
                      placeholder="Số nhà, tên đường, phường/xã, quận/huyện, tỉnh/thành..."
                      required
                      className="flex min-h-[70px] w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-2 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 resize-none disabled:bg-slate-100"
                    />
                  </div>

                  {/* Checkbox lưu thông tin nhận hàng cho lần sau */}
                  <div className="pt-1">
                    <label className="inline-flex items-center gap-2 cursor-pointer select-none text-xs text-slate-700 hover:text-slate-900">
                      <input
                        type="checkbox"
                        checked={saveForLater}
                        onChange={(e) => setSaveForLater(e.target.checked)}
                        disabled={isSubmitting}
                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                      />
                      <span className="font-medium">
                        Lưu thông tin giao hàng này vào sổ địa chỉ cho lần đặt hàng sau
                      </span>
                    </label>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-slate-900 block">
                    Ghi chú đơn hàng (Tùy chọn)
                  </label>
                  <div className="relative">
                    <FileText className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      name="note"
                      value={formData.note}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      placeholder="Giao giờ hành chính, gọi trước khi giao..."
                      className="flex h-11 w-full rounded-xl border border-slate-200 bg-slate-50 hover:bg-white pl-10 pr-3 py-1 text-sm shadow-xs transition-colors placeholder:text-slate-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-2 focus:ring-blue-500/20 disabled:bg-slate-100"
                    />
                  </div>
                </div>
              </div>

              {/* Card phương thức thanh toán */}
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-8 space-y-5">
                <div className="flex items-center gap-3 pb-4 border-b border-slate-100">
                  <span className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-xs shadow-xs">
                    2
                  </span>
                  <h2 className="text-base font-bold text-slate-900">Phương thức thanh toán</h2>
                </div>

                <div className="space-y-3.5">
                  <label
                    className={`flex items-start gap-3.5 p-4 rounded-2xl border transition-all cursor-pointer ${formData.paymentMethod === 'COD'
                      ? 'border-blue-600 bg-blue-50/50 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300'
                      } ${isSubmitting ? 'pointer-events-none opacity-70' : ''}`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={formData.paymentMethod === 'COD'}
                      onChange={handleChange}
                      disabled={isSubmitting}
                      className="mt-0.5 text-blue-600 focus:ring-blue-500 cursor-pointer"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <Banknote className="w-4 h-4 text-blue-600" />
                        <span className="font-bold text-sm text-slate-900">Thanh toán khi nhận hàng (COD)</span>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Thanh toán tiền mặt cho nhân viên giao hàng khi nhận được kiện hàng.
                      </p>
                    </div>
                  </label>

                  {/* Chuyển khoản VietQR tự động qua SePay (MBBank) */}
                  <div
                    className={`rounded-2xl border transition-all ${formData.paymentMethod === 'SEPAY'
                      ? 'border-emerald-600 bg-emerald-50/50 shadow-sm'
                      : 'border-slate-200 hover:border-slate-300'
                      }`}
                  >
                    <label
                      className={`flex items-start gap-3.5 p-4 cursor-pointer ${isSubmitting ? 'pointer-events-none opacity-70' : ''
                        }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value="SEPAY"
                        checked={formData.paymentMethod === 'SEPAY'}
                        onChange={handleChange}
                        disabled={isSubmitting}
                        className="mt-0.5 text-emerald-600 focus:ring-emerald-500 cursor-pointer"
                      />
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <QrCode className="w-4 h-4 text-emerald-600" />
                          <span className="font-bold text-sm text-slate-900">
                            Chuyển khoản VietQR qua SePay (MBBank)
                          </span>
                          <span className="text-[10px] uppercase font-extrabold tracking-wider px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700">
                            Tự động 24/7
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-0.5">
                          Quét mã VietQR trên ứng dụng ngân hàng. Hệ thống tự động xác nhận đơn hàng ngay khi nhận được tiền.
                        </p>
                      </div>
                    </label>

                    {formData.paymentMethod === 'SEPAY' && (
                      <div className="px-4 pb-4 pt-2 border-t border-emerald-200/60 pl-11 space-y-1.5 text-xs text-slate-600">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-700">Ngân hàng:</span>
                          <span className="font-bold text-slate-900">MBBank (Ngân Hàng TMCP Quân Đội)</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-700">Số tài khoản:</span>
                          <span className="font-mono font-bold text-slate-900">0372987662</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-slate-700">Chủ tài khoản:</span>
                          <span className="font-bold text-slate-900">DANG THANH BINH</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: Order summary (5/12) */}
            <div className="lg:col-span-5 space-y-6">
              <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 space-y-5 sticky top-24">
                <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                  <div className="flex items-center gap-2">
                    <ShoppingBag className="w-4 h-4 text-blue-600" />
                    <h2 className="text-base font-bold text-slate-900">Đơn hàng của bạn</h2>
                  </div>
                  <span className="text-xs font-bold px-3 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-200">
                    {totalCount} sản phẩm
                  </span>
                </div>

                {/* Danh sách sản phẩm mua */}
                <div className="max-h-64 overflow-y-auto divide-y divide-slate-100 pr-1">
                  {checkoutItems.map((item) => (
                    <div key={`${item.id}-${item.selectedSize || 'default'}`} className="py-3 flex items-center justify-between gap-3">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-11 h-11 rounded-xl bg-slate-50 shrink-0 overflow-hidden border border-slate-200 p-1 flex items-center justify-center">
                          {item.imageUrl ? (
                            <img src={item.imageUrl} alt={item.name} className="w-full h-full object-contain mix-blend-multiply" />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs font-bold">
                              SP
                            </div>
                          )}
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-slate-900 truncate">{item.name}</p>
                          {item.selectedSize && (
                            <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                              Size: {item.selectedSize}
                            </span>
                          )}
                          <p className="text-[11px] text-slate-500">
                            {formatPrice(item.price)} × {item.quantity}
                          </p>
                        </div>
                      </div>
                      <div className="text-xs font-extrabold text-slate-900 shrink-0 tabular-nums">
                        {formatPrice(item.price * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                {/* 🎟️ Mã giảm giá / Voucher (Giao diện Shopee) */}
                <div className="pt-3 border-t border-slate-100 space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                      <Ticket className="w-3.5 h-3.5 text-orange-500 fill-orange-400" />
                      <span>Mã giảm giá / Voucher</span>
                    </label>

                    {appliedCoupon && (
                      <button
                        type="button"
                        onClick={() => setIsVoucherModalOpen(true)}
                        className="text-xs font-bold text-blue-600 hover:text-blue-700 cursor-pointer"
                      >
                        Chọn mã khác
                      </button>
                    )}
                  </div>

                  {appliedCoupon ? (
                    <div className="p-3.5 rounded-2xl bg-gradient-to-r from-emerald-50 via-teal-50 to-emerald-50/80 border border-emerald-200 flex items-center justify-between text-xs shadow-2xs">
                      <div className="flex items-center gap-2.5 min-w-0 flex-1 pr-2">
                        <div className="w-8 h-8 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-xs">
                          <Ticket className="w-4 h-4" />
                        </div>
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="font-black text-emerald-950 tracking-wider text-xs sm:text-sm">
                              {appliedCoupon.code}
                            </span>
                            <span className="px-2 py-0.5 rounded-full bg-emerald-200 text-emerald-900 text-[10px] font-black">
                              -{formatPrice(appliedCoupon.discountAmount)}
                            </span>
                          </div>
                          <p className="text-[11px] text-emerald-700 line-clamp-1 mt-0.5 font-medium">
                            {appliedCoupon.message || 'Mã giảm giá đã được áp dụng'}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          type="button"
                          onClick={() => setIsVoucherModalOpen(true)}
                          className="text-blue-600 hover:text-blue-700 font-bold text-xs underline cursor-pointer hover:opacity-80"
                        >
                          Thay đổi
                        </button>
                        <button
                          type="button"
                          onClick={handleRemoveCoupon}
                          className="text-slate-400 hover:text-red-600 p-1.5 rounded-full hover:bg-red-50 transition-colors cursor-pointer"
                          title="Bỏ áp dụng voucher"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div
                      onClick={() => setIsVoucherModalOpen(true)}
                      className="p-3.5 rounded-2xl border border-dashed border-orange-300 bg-orange-50/40 hover:bg-orange-50 transition-all flex items-center justify-between cursor-pointer group shadow-2xs"
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-7 h-7 rounded-lg bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
                          <Ticket className="w-4 h-4" />
                        </div>
                        <span className="text-xs sm:text-sm font-bold text-slate-800 group-hover:text-orange-700 transition-colors">
                          Chọn voucher giảm giá
                        </span>
                      </div>
                      <div className="flex items-center gap-1 text-orange-600 font-bold text-xs">
                        <span>Chọn voucher</span>
                        <ChevronRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
                      </div>
                    </div>
                  )}

                  {couponError && (
                    <p className="text-[11px] text-red-600 font-medium flex items-center gap-1 pt-0.5">
                      <AlertCircle className="w-3 h-3 shrink-0" />
                      <span>{couponError}</span>
                    </p>
                  )}
                </div>

                {/* Chi phí */}
                <div className="pt-3 border-t border-slate-100 space-y-2.5 text-xs">
                  <div className="flex justify-between text-slate-600">
                    <span>Tạm tính</span>
                    <span className="font-bold text-slate-900 tabular-nums">{formatPrice(totalPrice)}</span>
                  </div>
                  {appliedCoupon && (
                    <div className="flex justify-between text-emerald-700">
                      <span>Giảm giá voucher ({appliedCoupon.code})</span>
                      <span className="font-bold tabular-nums">-{formatPrice(appliedCoupon.discountAmount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-slate-600">
                    <span className="flex items-center gap-1.5">
                      <span>Phí vận chuyển</span>
                      <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 font-semibold">Cố định</span>
                    </span>
                    <span className="font-bold text-slate-900 tabular-nums">16.000 ₫</span>
                  </div>
                  <div className="pt-3 border-t border-slate-100 flex justify-between text-sm">
                    <span className="font-bold text-slate-900">Tổng thanh toán</span>
                    <span className="font-extrabold text-slate-900 text-xl tabular-nums">{formatPrice(finalPrice)}</span>
                  </div>
                </div>

                {/* Submit button: Vibrant Orange Pill */}
                <button
                  type="submit"
                  disabled={isSubmitting || checkoutItems.length === 0}
                  className="w-full py-3.5 px-6 bg-orange-500 hover:bg-orange-600 active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none text-white font-bold text-sm rounded-full shadow-md shadow-orange-500/25 transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  {isSubmitting ? (
                    <>
                      <LoadingSpinner size="sm" className="text-white" />
                      <span>{isBuyNow ? 'Đang xử lý mua ngay...' : 'Đang xử lý đặt hàng...'}</span>
                    </>
                  ) : (
                    <>
                      {formData.paymentMethod === 'SEPAY' ? (
                        <QrCode className="w-4 h-4" />
                      ) : (
                        <Lock className="w-4 h-4" />
                      )}
                      <span>
                        {formData.paymentMethod === 'SEPAY'
                          ? 'Tiến hành chuyển khoản VietQR SePay'
                          : isBuyNow
                          ? 'Xác nhận Mua ngay (COD)'
                          : 'Xác nhận đặt hàng (COD)'}
                      </span>
                    </>
                  )}
                </button>

                <div className="text-center text-[11px] text-slate-400 leading-relaxed">
                  Bằng việc hoàn tất đơn, bạn xác nhận đồng ý với điều khoản mua sắm và chính sách bảo hành của Rikkei Store.
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>

      {/* Modal / Bottom Sheet Chọn Voucher kiểu Shopee */}
      <VoucherPickerModal
        isOpen={isVoucherModalOpen}
        onClose={() => setIsVoucherModalOpen(false)}
        onSelectVoucher={(data) => {
          setAppliedCoupon(data)
          setCouponError(null)
        }}
        currentCode={appliedCoupon?.code}
        orderValue={totalPrice}
      />

      {/* Modal Bản đồ tương tác OpenStreetMap Leaflet */}
      <AddressMapModal
        isOpen={isMapModalOpen}
        onClose={() => setIsMapModalOpen(false)}
        onSelectAddress={handleSelectMapAddress}
        initialAddress={formData.shippingAddress}
      />

      {/* Modal Sổ địa chỉ giao hàng đã lưu */}
      <SavedAddressModal
        isOpen={isSavedAddressModalOpen}
        onClose={() => {
          setIsSavedAddressModalOpen(false)
          setSavedCount(getSavedAddresses(user?.id).length)
        }}
        onSelectAddress={handleSelectSavedAddress}
        userId={user?.id}
        currentAddress={formData.shippingAddress}
      />
    </div>
  )
}

export default CheckoutPage
