import { createSlice, createAsyncThunk, createSelector } from '@reduxjs/toolkit';
import { 
  getWishlist, 
  addToWishlist as addToWishlistApi, 
  removeFromWishlist as removeFromWishlistApi, 
  clearWishlist as clearWishlistApi,
  moveToCart as moveToCartApi 
} from '../../api/wishlistApi';
import { addItemToCart } from '../cart/cartSlice';

// Async thunks
export const fetchWishlist = createAsyncThunk(
  'wishlist/fetchWishlist',
  async (_, { rejectWithValue, getState }) => {
    const { auth } = getState();
    if (!auth.isAuthenticated) {
      return rejectWithValue('Not authenticated');
    }
    try {
      const response = await getWishlist();
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to fetch wishlist';
      return rejectWithValue(message);
    }
  }
);

export const addItemToWishlist = createAsyncThunk(
  'wishlist/addItem',
  async ({ skuCode, name, price, imageUrl = '', productId = null }, { rejectWithValue, dispatch }) => {
    try {
      await addToWishlistApi(skuCode, name, price, imageUrl, productId);
      // Refetch wishlist to get updated data
      dispatch(fetchWishlist());
      return { skuCode, name, price, imageUrl, productId };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to add item to wishlist';
      return rejectWithValue(message);
    }
  }
);

export const removeItemFromWishlist = createAsyncThunk(
  'wishlist/removeItem',
  async (skuCode, { rejectWithValue, dispatch }) => {
    try {
      await removeFromWishlistApi(skuCode);
      // Refetch wishlist to get updated data
      dispatch(fetchWishlist());
      return skuCode;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to remove item from wishlist';
      return rejectWithValue(message);
    }
  }
);

export const moveItemToCart = createAsyncThunk(
  'wishlist/moveToCart',
  async (skuCode, { rejectWithValue, dispatch, getState }) => {
    try {
      // Get item details from wishlist before removing
      const { wishlist } = getState();
      const item = wishlist.items.find(i => i.skuCode === skuCode);
      
      // Call API to remove from wishlist
      const response = await moveToCartApi(skuCode);
      
      // Add item to cart using the item details
      if (item) {
        dispatch(addItemToCart({
          skuCode: item.skuCode,
          quantity: 1,
          imageUrl: item.imageUrl || '',
          price: item.price || 0
        }));
      }
      
      // Refetch wishlist to get updated data
      dispatch(fetchWishlist());
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to move item to cart';
      return rejectWithValue(message);
    }
  }
);

export const clearWishlistItems = createAsyncThunk(
  'wishlist/clearWishlist',
  async (_, { rejectWithValue }) => {
    try {
      await clearWishlistApi();
      return null;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to clear wishlist';
      return rejectWithValue(message);
    }
  }
);

const initialState = {
  items: [],
  totalItems: 0,
  isLoading: false,
  updatingSkuCodes: [],
  removingItemSku: null,
  movingToCartSku: null,
  error: null,
  successMessage: null,
};

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    clearSuccessMessage: (state) => {
      state.successMessage = null;
    },
    resetWishlist: (state) => {
      state.items = [];
      state.totalItems = 0;
      state.isLoading = false;
      state.updatingSkuCodes = [];
      state.removingItemSku = null;
      state.movingToCartSku = null;
      state.error = null;
      state.successMessage = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch wishlist
      .addCase(fetchWishlist.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.isLoading = false;
        state.items = action.payload?.items || [];
        state.totalItems = action.payload?.totalItems || 0;
      })
      .addCase(fetchWishlist.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload;
      })
      // Add item
      .addCase(addItemToWishlist.pending, (state, action) => {
        const skuCode = action.meta.arg?.skuCode;
        if (skuCode && !state.updatingSkuCodes.includes(skuCode)) {
          state.updatingSkuCodes.push(skuCode);
        }
        state.error = null;
      })
      .addCase(addItemToWishlist.fulfilled, (state, action) => {
        const skuCode = action.meta.arg?.skuCode;
        state.updatingSkuCodes = state.updatingSkuCodes.filter((code) => code !== skuCode);
        state.successMessage = 'Item added to wishlist!';
      })
      .addCase(addItemToWishlist.rejected, (state, action) => {
        const skuCode = action.meta.arg?.skuCode;
        state.updatingSkuCodes = state.updatingSkuCodes.filter((code) => code !== skuCode);
        state.error = action.payload;
      })
      // Remove item
      .addCase(removeItemFromWishlist.pending, (state, action) => {
        const skuCode = action.meta.arg;
        state.removingItemSku = skuCode;
        if (skuCode && !state.updatingSkuCodes.includes(skuCode)) {
          state.updatingSkuCodes.push(skuCode);
        }
        state.error = null;
      })
      .addCase(removeItemFromWishlist.fulfilled, (state, action) => {
        const skuCode = action.payload;
        state.updatingSkuCodes = state.updatingSkuCodes.filter((code) => code !== skuCode);
        state.removingItemSku = null;
        state.successMessage = 'Item removed from wishlist';
      })
      .addCase(removeItemFromWishlist.rejected, (state, action) => {
        const skuCode = action.meta.arg;
        state.updatingSkuCodes = state.updatingSkuCodes.filter((code) => code !== skuCode);
        state.removingItemSku = null;
        state.error = action.payload;
      })
      // Move to cart
      .addCase(moveItemToCart.pending, (state, action) => {
        state.movingToCartSku = action.meta.arg;
        state.error = null;
      })
      .addCase(moveItemToCart.fulfilled, (state) => {
        state.movingToCartSku = null;
        state.successMessage = 'Item moved to cart!';
      })
      .addCase(moveItemToCart.rejected, (state, action) => {
        state.movingToCartSku = null;
        state.error = action.payload;
      })
      // Clear wishlist
      .addCase(clearWishlistItems.fulfilled, (state) => {
        state.items = [];
        state.totalItems = 0;
        state.successMessage = 'Wishlist cleared';
      })
      .addCase(clearWishlistItems.rejected, (state, action) => {
        state.error = action.payload;
      });
  },
});

export const { clearError, clearSuccessMessage, resetWishlist } = wishlistSlice.actions;

// Selectors
export const selectWishlistItems = (state) => state.wishlist.items;
export const selectWishlistTotalItems = (state) => state.wishlist.totalItems;
export const selectWishlistLoading = (state) => state.wishlist.isLoading;
export const selectWishlistAddingItem = (state) => state.wishlist.updatingSkuCodes.length > 0;
export const selectIsWishlistUpdatingBySku = (skuCode) => (state) =>
  Boolean(skuCode) && state.wishlist.updatingSkuCodes.includes(skuCode);
export const selectWishlistRemovingItem = (state) => state.wishlist.removingItemSku;
export const selectWishlistMovingToCart = (state) => state.wishlist.movingToCartSku;
export const selectWishlistError = (state) => state.wishlist.error;
export const selectWishlistSuccessMessage = (state) => state.wishlist.successMessage;

// Memoized selector for wishlist SKU codes
export const selectWishlistSkuCodes = createSelector(
  [selectWishlistItems],
  (items) => items.map((item) => item.skuCode)
);

// Check if a specific item is in wishlist
export const selectIsItemInWishlist = (skuCode) => 
  createSelector(
    [selectWishlistSkuCodes],
    (skuCodes) => skuCodes.includes(skuCode)
  );

export default wishlistSlice.reducer;
