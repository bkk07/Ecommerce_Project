import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/checkout';
const PAYMENT_API_URL = 'http://localhost:8080/api/payments';

// Create axios instance with auth interceptor
const checkoutClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Create axios instance for payment API
const paymentClient = axios.create({
  baseURL: PAYMENT_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to all requests
checkoutClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add token to payment requests
paymentClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

/**
 * Initiate checkout from cart
 * @param {string} shippingAddress - JSON string of shipping address
 * @returns {Promise<{status: string, razorpayOrderId: string, amount: number, currency: string, failureReason: string, itemErrors: Array}>}
 */
export const initiateCheckoutFromCart = async (shippingAddress = '') => {
  const response = await checkoutClient.post('/initiate', {
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
  const response = await checkoutClient.post('/initiate', {
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
  const response = await paymentClient.post('/verify', {
    razorpayOrderId,
    razorpayPaymentId,
    razorpaySignature
  });
  return response.data;
};

export default checkoutClient;
