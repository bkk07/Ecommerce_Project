import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { fetchProductsByCategory, searchProductsByKeyword, searchProductsWithFilters } from '../../api/productApi';

// Async thunk for fetching products by category
export const fetchProducts = createAsyncThunk(
  'products/fetchProducts',
  async (category = 'Smartphones', { rejectWithValue }) => {
    try {
      const data = await fetchProductsByCategory(category);
      // API returns { products: { content: [...] }, categoryFacets, brandFacets }
      return data.products?.content || [];
    } catch (error) {
      return rejectWithValue(error.response?.data || 'Failed to fetch products');
    }
  }
);

// Async thunk for searching products by keyword
export const searchProducts = createAsyncThunk(
  'products/searchProducts',
  async (keyword, { rejectWithValue }) => {
    try {
      const data = await searchProductsByKeyword(keyword);
      return {
        products: data.products?.content || [],
        keyword,
        totalElements: data.products?.totalElements || 0,
        totalPages: data.products?.totalPages || 0,
        categoryFacets: data.categoryFacets || {},
        brandFacets: data.brandFacets || {},
      };
    } catch (error) {
      return rejectWithValue(error.response?.data || 'Failed to search products');
    }
  }
);

// Async thunk for searching with filters
export const searchProductsFiltered = createAsyncThunk(
  'products/searchProductsFiltered',
  async (params, { rejectWithValue }) => {
    try {
      const data = await searchProductsWithFilters(params);
      return {
        products: data.products?.content || [],
        keyword: params.keyword || '',
        totalElements: data.products?.totalElements || 0,
        totalPages: data.products?.totalPages || 0,
        currentPage: data.products?.number || 0,
        categoryFacets: data.categoryFacets || {},
        brandFacets: data.brandFacets || {},
        filters: params,
      };
    } catch (error) {
      return rejectWithValue(error.response?.data || 'Failed to search products');
    }
  }
);

// Async thunk for fetching category products with filters
export const fetchCategoryProductsFiltered = createAsyncThunk(
  'products/fetchCategoryProductsFiltered',
  async (params, { rejectWithValue }) => {
    try {
      const data = await searchProductsWithFilters(params);
      return {
        products: data.products?.content || [],
        category: params.category || '',
        totalElements: data.products?.totalElements || 0,
        totalPages: data.products?.totalPages || 0,
        currentPage: data.products?.number || 0,
        categoryFacets: data.categoryFacets || {},
        brandFacets: data.brandFacets || {},
        filters: params,
      };
    } catch (error) {
      return rejectWithValue(error.response?.data || 'Failed to fetch products');
    }
  }
);

const initialState = {
  items: [],
  searchResults: [],
  searchKeyword: '',
  status: 'idle', // 'idle' | 'loading' | 'succeeded' | 'failed'
  searchStatus: 'idle',
  error: null,
  searchError: null,
  currentCategory: 'Smartphones',
  pagination: {
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
  },
  categoryFacets: {},
  brandFacets: {},
  filters: {
    category: '',
    minPrice: '',
    maxPrice: '',
    minRating: '',
    sortBy: '',
    sortOrder: 'asc',
  },
};

const productSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    setCategory: (state, action) => {
      state.currentCategory = action.payload;
    },
    clearProducts: (state) => {
      state.items = [];
      state.status = 'idle';
      state.error = null;
    },
    clearSearch: (state) => {
      state.searchResults = [];
      state.searchKeyword = '';
      state.searchStatus = 'idle';
      state.searchError = null;
    },
    setFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = {
        category: '',
        minPrice: '',
        maxPrice: '',
        minRating: '',
        sortBy: '',
        sortOrder: 'asc',
      };
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProducts.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchProducts.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload;
        state.error = null;
      })
      .addCase(fetchProducts.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      })
      // Search products cases
      .addCase(searchProducts.pending, (state) => {
        state.searchStatus = 'loading';
        state.searchError = null;
      })
      .addCase(searchProducts.fulfilled, (state, action) => {
        state.searchStatus = 'succeeded';
        state.searchResults = action.payload.products;
        state.searchKeyword = action.payload.keyword;
        state.pagination = {
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          currentPage: 0,
        };
        state.categoryFacets = action.payload.categoryFacets;
        state.brandFacets = action.payload.brandFacets;
        state.searchError = null;
      })
      .addCase(searchProducts.rejected, (state, action) => {
        state.searchStatus = 'failed';
        state.searchError = action.payload;
      })
      // Filtered search cases
      .addCase(searchProductsFiltered.pending, (state) => {
        state.searchStatus = 'loading';
        state.searchError = null;
      })
      .addCase(searchProductsFiltered.fulfilled, (state, action) => {
        state.searchStatus = 'succeeded';
        state.searchResults = action.payload.products;
        state.searchKeyword = action.payload.keyword;
        state.pagination = {
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          currentPage: action.payload.currentPage,
        };
        state.categoryFacets = action.payload.categoryFacets;
        state.brandFacets = action.payload.brandFacets;
        state.filters = action.payload.filters;
        state.searchError = null;
      })
      .addCase(searchProductsFiltered.rejected, (state, action) => {
        state.searchStatus = 'failed';
        state.searchError = action.payload;
      })
      // Category products with filters cases
      .addCase(fetchCategoryProductsFiltered.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchCategoryProductsFiltered.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload.products;
        state.currentCategory = action.payload.category;
        state.pagination = {
          totalElements: action.payload.totalElements,
          totalPages: action.payload.totalPages,
          currentPage: action.payload.currentPage,
        };
        state.categoryFacets = action.payload.categoryFacets;
        state.brandFacets = action.payload.brandFacets;
        state.error = null;
      })
      .addCase(fetchCategoryProductsFiltered.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      });
  },
});

export const { setCategory, clearProducts, clearSearch, setFilters, clearFilters } = productSlice.actions;

// Selectors
export const selectAllProducts = (state) => state.products.items;
export const selectProductsStatus = (state) => state.products.status;
export const selectProductsError = (state) => state.products.error;
export const selectCurrentCategory = (state) => state.products.currentCategory;
export const selectSearchResults = (state) => state.products.searchResults;
export const selectSearchStatus = (state) => state.products.searchStatus;
export const selectSearchKeyword = (state) => state.products.searchKeyword;
export const selectSearchError = (state) => state.products.searchError;
export const selectPagination = (state) => state.products.pagination;
export const selectCategoryFacets = (state) => state.products.categoryFacets;
export const selectBrandFacets = (state) => state.products.brandFacets;
export const selectFilters = (state) => state.products.filters;

export default productSlice.reducer;
