import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { loginUser, registerUser, verifyEmailOtp, sendEmailVerificationOtp, logoutUser } from '../../api/authApi';
import { storeTokens, clearAuth } from '../../api/apiClient';

// Helper functions for localStorage
const getStoredAuth = () => {
  try {
    const token = localStorage.getItem('token');
    const refreshToken = localStorage.getItem('refreshToken');
    const userId = localStorage.getItem('userId');
    const role = localStorage.getItem('role');
    if (token && userId) {
      return { token, refreshToken, userId: Number(userId), role };
    }
  } catch (error) {
    console.error('Error reading from localStorage:', error);
  }
  return null;
};

const storeAuth = (authData) => {
  storeTokens(authData.token, authData.refreshToken, authData.expiresIn);
  localStorage.setItem('userId', authData.userId.toString());
  localStorage.setItem('role', authData.role || 'USER');
};

const clearStoredAuth = () => {
  clearAuth();
};

// Async thunks
export const login = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await loginUser(credentials);
      // Check if verification is required (no token returned)
      if (response.requiresVerification) {
        return { 
          requiresVerification: true, 
          userId: response.userId, 
          email: credentials.email,
          message: response.message 
        };
      }
      storeAuth(response);
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Login failed. Please try again.';
      return rejectWithValue({ message });
    }
  }
);

export const register = createAsyncThunk(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    try {
      const response = await registerUser(userData);
      // Registration always returns success with userId (industry standard - no email enumeration)
      // If userId is 0, account already exists but we don't reveal this to prevent enumeration
      return { 
        ...response, 
        email: userData.email, 
        requiresVerification: true,
        // Use the generic message from backend
        genericMessage: response.message || 'If this email is not already registered, you will receive a verification email shortly.'
      };
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Registration failed. Please try again.';
      return rejectWithValue({ message });
    }
  }
);

export const verifyEmail = createAsyncThunk(
  'auth/verifyEmail',
  async ({ userId, otp }, { rejectWithValue }) => {
    try {
      const response = await verifyEmailOtp(userId, otp);
      storeAuth(response);
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Verification failed. Please try again.';
      return rejectWithValue({ message });
    }
  }
);

export const resendVerificationOtp = createAsyncThunk(
  'auth/resendOtp',
  async (userId, { rejectWithValue }) => {
    try {
      const response = await sendEmailVerificationOtp(userId);
      return response;
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to resend OTP.';
      return rejectWithValue({ message });
    }
  }
);

// Initial state
const storedAuth = getStoredAuth();
const initialState = {
  user: storedAuth ? { userId: storedAuth.userId, role: storedAuth.role } : null,
  token: storedAuth?.token || null,
  refreshToken: storedAuth?.refreshToken || null,
  isAuthenticated: !!storedAuth,
  isLoading: false,
  error: null,
  // Email verification state
  pendingVerification: null, // { userId, email }
  verificationSuccess: false,
  resendSuccess: false,
};

// Async thunk for logout
export const performLogout = createAsyncThunk(
  'auth/performLogout',
  async (_, { getState }) => {
    const { refreshToken } = getState().auth;
    if (refreshToken) {
      try {
        await logoutUser(refreshToken);
      } catch (error) {
        // Ignore errors - we still want to clear local state
        console.warn('Logout API call failed:', error);
      }
    }
    clearStoredAuth();
    return null;
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      state.error = null;
      state.pendingVerification = null;
      state.verificationSuccess = false;
      clearStoredAuth();
    },
    clearError: (state) => {
      state.error = null;
    },
    clearVerificationState: (state) => {
      state.pendingVerification = null;
      state.verificationSuccess = false;
      state.resendSuccess = false;
    },
    setPendingVerification: (state, action) => {
      state.pendingVerification = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // Login cases
      .addCase(login.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.isLoading = false;
        // Check if verification is required
        if (action.payload.requiresVerification) {
          state.isAuthenticated = false;
          state.pendingVerification = {
            userId: action.payload.userId,
            email: action.payload.email,
            fromLogin: true
          };
          state.error = null;
        } else {
          state.isAuthenticated = true;
          state.token = action.payload.token;
          state.refreshToken = action.payload.refreshToken;
          state.user = {
            userId: action.payload.userId,
            role: action.payload.role,
          };
          state.error = null;
          state.pendingVerification = null;
        }
      })
      .addCase(login.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload?.message || 'Login failed';
      })
      // Register cases
      .addCase(register.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = false; // Not authenticated until email verified
        state.pendingVerification = {
          userId: action.payload.userId,
          email: action.payload.email,
        };
        state.error = null;
      })
      .addCase(register.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload?.message || 'Registration failed';
      })
      // Verify email cases
      .addCase(verifyEmail.pending, (state) => {
        state.isLoading = true;
        state.error = null;
      })
      .addCase(verifyEmail.fulfilled, (state, action) => {
        state.isLoading = false;
        state.isAuthenticated = true;
        state.token = action.payload.token;
        state.refreshToken = action.payload.refreshToken;
        state.user = {
          userId: action.payload.userId,
          role: action.payload.role,
        };
        state.pendingVerification = null;
        state.verificationSuccess = true;
        state.error = null;
      })
      .addCase(verifyEmail.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload?.message || 'Verification failed';
      })
      // Resend OTP cases
      .addCase(resendVerificationOtp.pending, (state) => {
        state.resendSuccess = false;
      })
      .addCase(resendVerificationOtp.fulfilled, (state) => {
        state.resendSuccess = true;
      })
      .addCase(resendVerificationOtp.rejected, (state, action) => {
        state.error = action.payload?.message || 'Failed to resend OTP';
      })
      // Logout cases
      .addCase(performLogout.fulfilled, (state) => {
        state.user = null;
        state.token = null;
        state.refreshToken = null;
        state.isAuthenticated = false;
        state.error = null;
        state.pendingVerification = null;
        state.verificationSuccess = false;
      });
  },
});

export const { logout, clearError, clearVerificationState, setPendingVerification } = authSlice.actions;

// Selectors
export const selectAuth = (state) => state.auth;
export const selectIsAuthenticated = (state) => state.auth.isAuthenticated;
export const selectUser = (state) => state.auth.user;
export const selectAuthLoading = (state) => state.auth.isLoading;
export const selectAuthError = (state) => state.auth.error;
export const selectPendingVerification = (state) => state.auth.pendingVerification;
export const selectVerificationSuccess = (state) => state.auth.verificationSuccess;
export const selectResendSuccess = (state) => state.auth.resendSuccess;
export const selectRefreshToken = (state) => state.auth.refreshToken;

export default authSlice.reducer;
