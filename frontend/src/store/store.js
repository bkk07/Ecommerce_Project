import { configureStore } from '@reduxjs/toolkit';
import productReducer from '../features/products/productSlice';
import authReducer from '../features/auth/authSlice';
import cartReducer from '../features/cart/cartSlice';
import wishlistReducer from '../features/wishlist/wishlistSlice';
import adminReducer from '../features/admin/adminSlice';
import adminOrderReducer from '../features/admin/adminOrderSlice';

export const store = configureStore({
  reducer: {
    products: productReducer,
    auth: authReducer,
    cart: cartReducer,
    wishlist: wishlistReducer,
    admin: adminReducer,
    adminOrders: adminOrderReducer,
  },
});

export default store;
