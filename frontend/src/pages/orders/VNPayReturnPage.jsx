import React from 'react'
import PaymentResultPage from './PaymentResultPage'

/**
 * Trang nhận kết quả chuyển hướng từ Cổng thanh toán VNPay Sandbox.
 * Hứng mã vnp_ResponseCode hiển thị kết quả thành công/thất bại và chi tiết giao dịch.
 * Phụ trách: Đặng Thanh Bình (DTB) | Phối hợp: Đỗ Đức Mạnh (DDM)
 */
export const VNPayReturnPage = () => {
  return <PaymentResultPage />
}

export default VNPayReturnPage
