import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { getUserStats } from '../../api/adminApi';

// Async thunk to fetch user statistics
export const fetchUserStats = createAsyncThunk(
  'admin/fetchUserStats',
  async (_, { rejectWithValue }) => {
    try {
      const response = await getUserStats();
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to fetch user statistics';
      return rejectWithValue({ message });
    }
  }
);

// Initial state
const initialState = {
  userStats: null,
  isLoading: false,
  error: null,
  lastFetched: null,
};

const adminSlice = createSlice({
  name: 'admin',
  initialState,
  reducers: {
    clearAdminError: (state) => {
      state.error = null;
    },
    clearUserStats: (state) => {
      state.userStats = null;
      state.lastFetched = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUserStats.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(fetchUserStats.fulfilled, (state, action) => {
        state.isLoading = false;
        state.userStats = action.payload;
        state.lastFetched = new Date().toISOString();
        state.error = null;
      })
      .addCase(fetchUserStats.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload?.message || 'Failed to fetch statistics';
      });
  },
});

export const { clearAdminError, clearUserStats } = adminSlice.actions;

// Selectors
export const selectUserStats = (state) => state.admin.userStats;
export const selectAdminLoading = (state) => state.admin.isLoading;
export const selectAdminError = (state) => state.admin.error;
export const selectLastFetched = (state) => state.admin.lastFetched;

export default adminSlice.reducer;
