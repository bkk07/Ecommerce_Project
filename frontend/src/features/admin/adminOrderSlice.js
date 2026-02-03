import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
  fetchOrderStats,
  fetchAllOrders,
  fetchOrdersByStatus,
  fetchOrdersByDateRange,
  searchOrders,
  fetchOrderById,
} from "../../api/adminOrderApi";

// Async thunks
export const getOrderStats = createAsyncThunk(
  "adminOrders/getStats",
  async (_, { rejectWithValue }) => {
    try {
      const data = await fetchOrderStats();
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch order stats");
    }
  }
);

export const getAllOrders = createAsyncThunk(
  "adminOrders/getAll",
  async ({ page, size, sortBy, sortDirection }, { rejectWithValue }) => {
    try {
      const data = await fetchAllOrders(page, size, sortBy, sortDirection);
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch orders");
    }
  }
);

export const getOrdersByStatus = createAsyncThunk(
  "adminOrders/getByStatus",
  async ({ status, page, size }, { rejectWithValue }) => {
    try {
      const data = await fetchOrdersByStatus(status, page, size);
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch orders by status");
    }
  }
);

export const getOrdersByDateRange = createAsyncThunk(
  "adminOrders/getByDateRange",
  async ({ startDate, endDate, page, size }, { rejectWithValue }) => {
    try {
      const data = await fetchOrdersByDateRange(startDate, endDate, page, size);
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch orders by date range");
    }
  }
);

export const searchOrdersAction = createAsyncThunk(
  "adminOrders/search",
  async ({ keyword, page, size }, { rejectWithValue }) => {
    try {
      const data = await searchOrders(keyword, page, size);
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to search orders");
    }
  }
);

export const getOrderById = createAsyncThunk(
  "adminOrders/getById",
  async (orderId, { rejectWithValue }) => {
    try {
      const data = await fetchOrderById(orderId);
      return data;
    } catch (error) {
      return rejectWithValue(error.response?.data?.message || "Failed to fetch order details");
    }
  }
);

const initialState = {
  // Stats
  stats: null,
  statsLoading: false,
  statsError: null,

  // Orders list
  orders: [],
  currentPage: 0,
  totalPages: 0,
  totalElements: 0,
  pageSize: 10,
  hasNext: false,
  hasPrevious: false,
  ordersLoading: false,
  ordersError: null,

  // Selected order
  selectedOrder: null,
  selectedOrderLoading: false,
  selectedOrderError: null,
};

const adminOrderSlice = createSlice({
  name: "adminOrders",
  initialState,
  reducers: {
    clearSelectedOrder: (state) => {
      state.selectedOrder = null;
      state.selectedOrderError = null;
    },
    clearOrdersError: (state) => {
      state.ordersError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Get Order Stats
      .addCase(getOrderStats.pending, (state) => {
        state.statsLoading = true;
        state.statsError = null;
      })
      .addCase(getOrderStats.fulfilled, (state, action) => {
        state.statsLoading = false;
        state.stats = action.payload;
      })
      .addCase(getOrderStats.rejected, (state, action) => {
        state.statsLoading = false;
        state.statsError = action.payload;
      })

      // Get All Orders
      .addCase(getAllOrders.pending, (state) => {
        state.ordersLoading = true;
        state.ordersError = null;
      })
      .addCase(getAllOrders.fulfilled, (state, action) => {
        state.ordersLoading = false;
        state.orders = action.payload.orders;
        state.currentPage = action.payload.currentPage;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.pageSize = action.payload.pageSize;
        state.hasNext = action.payload.hasNext;
        state.hasPrevious = action.payload.hasPrevious;
      })
      .addCase(getAllOrders.rejected, (state, action) => {
        state.ordersLoading = false;
        state.ordersError = action.payload;
      })

      // Get Orders By Status
      .addCase(getOrdersByStatus.pending, (state) => {
        state.ordersLoading = true;
        state.ordersError = null;
      })
      .addCase(getOrdersByStatus.fulfilled, (state, action) => {
        state.ordersLoading = false;
        state.orders = action.payload.orders;
        state.currentPage = action.payload.currentPage;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.pageSize = action.payload.pageSize;
        state.hasNext = action.payload.hasNext;
        state.hasPrevious = action.payload.hasPrevious;
      })
      .addCase(getOrdersByStatus.rejected, (state, action) => {
        state.ordersLoading = false;
        state.ordersError = action.payload;
      })

      // Get Orders By Date Range
      .addCase(getOrdersByDateRange.pending, (state) => {
        state.ordersLoading = true;
        state.ordersError = null;
      })
      .addCase(getOrdersByDateRange.fulfilled, (state, action) => {
        state.ordersLoading = false;
        state.orders = action.payload.orders;
        state.currentPage = action.payload.currentPage;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.pageSize = action.payload.pageSize;
        state.hasNext = action.payload.hasNext;
        state.hasPrevious = action.payload.hasPrevious;
      })
      .addCase(getOrdersByDateRange.rejected, (state, action) => {
        state.ordersLoading = false;
        state.ordersError = action.payload;
      })

      // Search Orders
      .addCase(searchOrdersAction.pending, (state) => {
        state.ordersLoading = true;
        state.ordersError = null;
      })
      .addCase(searchOrdersAction.fulfilled, (state, action) => {
        state.ordersLoading = false;
        state.orders = action.payload.orders;
        state.currentPage = action.payload.currentPage;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.pageSize = action.payload.pageSize;
        state.hasNext = action.payload.hasNext;
        state.hasPrevious = action.payload.hasPrevious;
      })
      .addCase(searchOrdersAction.rejected, (state, action) => {
        state.ordersLoading = false;
        state.ordersError = action.payload;
      })

      // Get Order By ID
      .addCase(getOrderById.pending, (state) => {
        state.selectedOrderLoading = true;
        state.selectedOrderError = null;
      })
      .addCase(getOrderById.fulfilled, (state, action) => {
        state.selectedOrderLoading = false;
        state.selectedOrder = action.payload;
      })
      .addCase(getOrderById.rejected, (state, action) => {
        state.selectedOrderLoading = false;
        state.selectedOrderError = action.payload;
      });
  },
});

export const { clearSelectedOrder, clearOrdersError } = adminOrderSlice.actions;
export default adminOrderSlice.reducer;
