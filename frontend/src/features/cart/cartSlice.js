import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';
import { getCart, addToCart as addToCartApi, removeFromCart as removeFromCartApi, clearCart as clearCartApi, updateCartItemPrice as updateCartItemPriceApi } from '../../api/cartApi';

// Async thunks
export const fetchCart = createAsyncThunk(
  'cart/fetchCart',
  async (_, { rejectWithValue, getState }) => {
    const { auth } = getState();
    if (!auth.isAuthenticated) {
      return rejectWithValue('Not authenticated');
    }
    try {
      const response = await getCart();
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to fetch cart';
      return rejectWithValue(message);
    }
  }
);

export const addItemToCart = createAsyncThunk(
  'cart/addItem',
  async ({ skuCode, quantity = 1, imageUrl = '', price = 0 }, { rejectWithValue, dispatch }) => {
    try {
      await addToCartApi(skuCode, quantity, imageUrl, price);
      // Refetch cart to get updated data
      dispatch(fetchCart());
      return { skuCode, quantity, imageUrl, price };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to add item to cart';
      return rejectWithValue(message);
    }
  }
);

export const updateCartItemPrice = createAsyncThunk(
  'cart/updateItemPrice',
  async ({ skuCode, price }, { rejectWithValue, dispatch }) => {
    try {
      await updateCartItemPriceApi(skuCode, price);
      // Refetch cart to get updated data
      dispatch(fetchCart());
      return { skuCode, price };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to update item price';
      return rejectWithValue(message);
    }
  }
);

export const removeItemFromCart = createAsyncThunk(
  'cart/removeItem',
  async (skuCode, { rejectWithValue, dispatch }) => {
    try {
      await removeFromCartApi(skuCode);
      // Refetch cart to get updated data
      dispatch(fetchCart());
      return skuCode;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to remove item from cart';
      return rejectWithValue(message);
    }
  }
);

export const clearCartItems = createAsyncThunk(
  'cart/clearCart',
  async (_, { rejectWithValue }) => {
    try {
      await clearCartApi();
      return null;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to clear cart';
      return rejectWithValue(message);
    }
  }
);

const initialState = {
  items: [],
  totalAmount: 0,
  isLoading: false,
  isAddingItem: false,
  error: null,
  successMessage: null,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    clearSuccessMessage: (state) => {
      state.successMessage = null;
    },
    resetCart: (state) => {
      state.items = [];
      state.totalAmount = 0;
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch cart
      .addCase(fetchCart.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchCart.fulfilled, (state, action) => {
        state.isLoading = false;
        state.items = action.payload?.items || [];
        state.totalAmount = action.payload?.totalAmount || 0;
      })
      .addCase(fetchCart.rejected, (state, action) => {
        state.isLoading = false;
        // Don't show error for non-authenticated users
        if (action.payload !== 'Not authenticated') {
          state.error = action.payload;
        }
      })
      // Add item
      .addCase(addItemToCart.pending, (state) => {
        state.isAddingItem = true;
        state.error = null;
      })
      .addCase(addItemToCart.fulfilled, (state) => {
        state.isAddingItem = false;
        state.successMessage = 'Item added to cart!';
      })
      .addCase(addItemToCart.rejected, (state, action) => {
        state.isAddingItem = false;
        state.error = action.payload;
      })
      // Remove item
      .addCase(removeItemFromCart.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(removeItemFromCart.fulfilled, (state) => {
        state.isLoading = false;
        state.successMessage = 'Item removed from cart';
      })
      .addCase(removeItemFromCart.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Clear cart
      .addCase(clearCartItems.pending, (state) => {
        state.isLoading = true;
      })
      .addCase(clearCartItems.fulfilled, (state) => {
        state.isLoading = false;
        state.items = [];
        state.totalAmount = 0;
        state.successMessage = 'Cart cleared';
      })
      .addCase(clearCartItems.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Update item price
      .addCase(updateCartItemPrice.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(updateCartItemPrice.fulfilled, (state) => {
        state.isLoading = false;
        state.successMessage = 'Price updated';
      })
      .addCase(updateCartItemPrice.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      });
  },
});

export const { clearError, clearSuccessMessage, resetCart } = cartSlice.actions;

// Selectors
export const selectCartItems = (state) => state.cart.items;
export const selectCartTotal = (state) => state.cart.totalAmount;
export const selectCartItemCount = createSelector(
  [selectCartItems],
  (items) => items.reduce((total, item) => total + (item.quantity || 0), 0)
);
export const selectCartLoading = (state) => state.cart.isLoading;
export const selectCartError = (state) => state.cart.error;
export const selectAddingItem = (state) => state.cart.isAddingItem;
export const selectSuccessMessage = (state) => state.cart.successMessage;
export const selectIsItemInCart = (skuCode) => (state) => 
  state.cart.items.some((item) => item.skuCode === skuCode);
export const selectCartSkuCodes = createSelector(
  [selectCartItems],
  (items) => items.map((item) => item.skuCode)
);

export default cartSlice.reducer;
