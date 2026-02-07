import apiClient from './apiClient';

const CHECKOUT_BASE_PATH = '/api/checkout';
const PAYMENT_BASE_PATH = '/api/payments';

/**
 * Initiate checkout from cart
 * @param {string} shippingAddress - JSON string of shipping address
 * @returns {Promise<{status: string, razorpayOrderId: string, amount: number, currency: string, failureReason: string, itemErrors: Array}>}
 */
export const initiateCheckoutFromCart = async (shippingAddress = '') => {
  const response = await apiClient.post(`${CHECKOUT_BASE_PATH}/initiate`, {
    cartId: true,
    items: [],
    shippingAddress: shippingAddress
  });
  return response.data;
};

/**
 * Initiate checkout with specific items
 * @param {Array} items - Array of checkout items with skuCode, quantity, price, imageUrl
 * @param {string} shippingAddress - JSON string of shipping address
 * @returns {Promise<{status: string, razorpayOrderId: string, amount: number, currency: string, failureReason: string, itemErrors: Array}>}
 */
export const initiateCheckoutWithItems = async (items, shippingAddress = '') => {
  const response = await apiClient.post(`${CHECKOUT_BASE_PATH}/initiate`, {
    cartId: false,
    items: items.map(item => ({
      skuCode: item.skuCode,
      quantity: item.quantity,
      price: item.price,
      productName: item.productName || item.name || '',
      imageUrl: item.imageUrl || ''
    })),
    shippingAddress: shippingAddress
  });
  return response.data;
};

/**
 * Verify payment with backend after Razorpay success
 * @param {string} razorpayOrderId - Razorpay order ID
 * @param {string} razorpayPaymentId - Razorpay payment ID
 * @param {string} razorpaySignature - Razorpay signature for verification
 * @returns {Promise<{message: string, verified: boolean, orderId: string, razorpayOrderId: string, razorpayPaymentId: string, amount: number, currency: string, status: string}>} - Verification response with order details
 */
export const verifyPayment = async (razorpayOrderId, razorpayPaymentId, razorpaySignature) => {
  const response = await apiClient.post(`${PAYMENT_BASE_PATH}/verify`, {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature
  });
  return response.data;
};

export default apiClient;
