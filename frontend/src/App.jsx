import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import WishlistToast from './components/WishlistToast';
import AppRoutes from './routes/AppRoutes';
import AdminDashboard from './pages/AdminDashboard';
import { selectUser, selectIsAuthenticated, logout } from './features/auth/authSlice';

// Layout wrapper that conditionally shows Navbar/Footer
const MainLayout = ({ children }) => {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <Navbar />
      <WishlistToast />
      <main className="flex-1">
        {children}
      </main>
      <Footer />
    </div>
  );
};

// Component to handle routing logic
const AppContent = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const user = useSelector(selectUser);
  const isAuthenticated = useSelector(selectIsAuthenticated);
  
  // Listen for auth:logout event (from API client when refresh token fails)
  useEffect(() => {
    const handleAuthLogout = () => {
      dispatch(logout());
      navigate('/login', { replace: true });
    };
    
    window.addEventListener('auth:logout', handleAuthLogout);
    return () => window.removeEventListener('auth:logout', handleAuthLogout);
  }, [dispatch, navigate]);
  
  // Check if current path is admin route
  const isAdminRoute = location.pathname.startsWith('/admin');
  
  // If admin route and user is admin, render without normal layout
  if (isAdminRoute && isAuthenticated && user?.role === 'ADMIN') {
    return <AdminDashboard />;
  }
  
  // Normal routes with standard layout
  return (
    <MainLayout>
      <AppRoutes />
    </MainLayout>
  );
};

function App() {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
}

export default App;
