import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { 
  fetchWishlist, 
  removeItemFromWishlist, 
  moveItemToCart,
  clearWishlistItems,
  selectWishlistItems, 
  selectWishlistLoading, 
  selectWishlistError,
  selectWishlistRemovingItem,
  selectWishlistMovingToCart
} from '../features/wishlist/wishlistSlice';
import { selectIsAuthenticated } from '../features/auth/authSlice';

// Floating hearts animation component
const FloatingHearts = () => (
  <div className="absolute inset-0 overflow-hidden pointer-events-none">
    {[...Array(6)].map((_, i) => (
      <div
        key={i}
        className="absolute animate-float opacity-20"
        style={{
          left: `${15 + i * 15}%`,
          animationDelay: `${i * 0.5}s`,
          animationDuration: `${3 + i * 0.5}s`
        }}
      >
        <svg className="w-8 h-8 text-pink-400" fill="currentColor" viewBox="0 0 24 24">
          <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
        </svg>
      </div>
    ))}
  </div>
);

const Wishlist = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [hoveredItem, setHoveredItem] = useState(null);
  
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const items = useSelector(selectWishlistItems);
  const loading = useSelector(selectWishlistLoading);
  const error = useSelector(selectWishlistError);
  const removingItem = useSelector(selectWishlistRemovingItem);
  const movingToCart = useSelector(selectWishlistMovingToCart);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchWishlist());
    }
  }, [dispatch, isAuthenticated]);

  const handleRemoveItem = (skuCode) => {
    dispatch(removeItemFromWishlist(skuCode));
  };

  const handleMoveToCart = (skuCode) => {
    dispatch(moveItemToCart(skuCode));
  };

  const handleClearWishlist = () => {
    if (window.confirm('Are you sure you want to clear your entire wishlist?')) {
      dispatch(clearWishlistItems());
    }
  };

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4 bg-gradient-to-br from-pink-50 via-white to-purple-50">
        <div className="text-center relative">
          <FloatingHearts />
          <div className="relative">
            <div className="mx-auto w-28 h-28 bg-gradient-to-br from-pink-400 to-rose-500 rounded-full flex items-center justify-center mb-6 shadow-2xl shadow-pink-200 animate-pulse">
              <svg className="w-14 h-14 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
            <h2 className="text-3xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent mb-3">
              Sign in to view your wishlist
            </h2>
            <p className="text-gray-500 mb-8 text-lg">Discover your saved treasures</p>
            <button
              onClick={() => navigate('/login', { state: { from: '/wishlist' } })}
              className="px-8 py-4 bg-gradient-to-r from-pink-500 to-rose-500 text-white font-semibold rounded-2xl hover:from-pink-600 hover:to-rose-600 transition-all duration-300 transform hover:scale-105 hover:shadow-xl shadow-pink-300"
            >
              Sign In to Continue
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center bg-gradient-to-br from-pink-50 via-white to-purple-50">
        <div className="flex flex-col items-center space-y-6">
          <div className="relative">
            <div className="w-20 h-20 border-4 border-pink-200 rounded-full"></div>
            <div className="w-20 h-20 border-4 border-pink-500 border-t-transparent rounded-full animate-spin absolute top-0 left-0"></div>
            <div className="absolute inset-0 flex items-center justify-center">
              <svg className="w-8 h-8 text-pink-500 animate-pulse" fill="currentColor" viewBox="0 0 24 24">
                <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
              </svg>
            </div>
          </div>
          <p className="text-gray-600 font-medium animate-pulse">Loading your favorites...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4 bg-gradient-to-br from-red-50 via-white to-pink-50">
        <div className="text-center bg-white p-10 rounded-3xl shadow-xl border border-red-100 max-w-md">
          <div className="mx-auto w-20 h-20 bg-gradient-to-br from-red-400 to-rose-500 rounded-full flex items-center justify-center mb-6 shadow-lg">
            <svg className="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-3">Oops! Something went wrong</h2>
          <p className="text-gray-500 mb-6">{error}</p>
          <button
            onClick={() => dispatch(fetchWishlist())}
            className="px-8 py-3 bg-gradient-to-r from-pink-500 to-rose-500 text-white font-semibold rounded-xl hover:from-pink-600 hover:to-rose-600 transition-all duration-300 transform hover:scale-105"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!items || items.length === 0) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4 bg-gradient-to-br from-pink-50 via-white to-purple-50 relative">
        <FloatingHearts />
        <div className="text-center relative z-10">
          <div className="mx-auto w-32 h-32 bg-gradient-to-br from-gray-100 to-gray-200 rounded-full flex items-center justify-center mb-6 shadow-inner">
            <svg className="w-16 h-16 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
            </svg>
          </div>
          <h2 className="text-3xl font-bold text-gray-800 mb-3">Your wishlist is empty</h2>
          <p className="text-gray-500 mb-8 text-lg max-w-sm mx-auto">
            Start adding items you love by clicking the heart icon on products
          </p>
          <Link
            to="/search"
            className="inline-flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-pink-500 to-purple-500 text-white font-semibold rounded-2xl hover:from-pink-600 hover:to-purple-600 transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            Explore Products
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-white to-purple-50">
      {/* Decorative Header Background */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-r from-pink-100/50 to-purple-100/50"></div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-gradient-to-br from-pink-200/30 to-rose-200/30 rounded-full translate-x-1/2 -translate-y-1/2 blur-3xl"></div>
        <div className="absolute bottom-0 left-0 w-72 h-72 bg-gradient-to-tr from-purple-200/30 to-pink-200/30 rounded-full -translate-x-1/2 translate-y-1/2 blur-3xl"></div>
        
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 relative">
          {/* Header */}
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-gradient-to-br from-pink-500 to-rose-500 rounded-2xl flex items-center justify-center shadow-lg shadow-pink-200 transform -rotate-3">
                <svg className="w-9 h-9 text-white" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </div>
              <div>
                <h1 className="text-4xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
                  My Wishlist
                </h1>
                <p className="text-gray-500 mt-1 flex items-center gap-2">
                  <span className="inline-flex items-center justify-center w-6 h-6 bg-pink-100 text-pink-600 rounded-full text-sm font-semibold">
                    {items.length}
                  </span>
                  {items.length === 1 ? 'item' : 'items'} saved with love
                </p>
              </div>
            </div>
            <button
              onClick={handleClearWishlist}
              className="group flex items-center gap-2 px-5 py-2.5 bg-white/80 backdrop-blur-sm border border-red-200 text-red-500 font-medium rounded-xl hover:bg-red-50 hover:border-red-300 transition-all duration-300"
            >
              <svg className="w-5 h-5 group-hover:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              <span>Clear All</span>
            </button>
          </div>
        </div>
      </div>

      {/* Wishlist Grid */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {items.map((item, index) => (
            <div
              key={item.skuCode}
              className="group relative bg-white rounded-3xl shadow-sm hover:shadow-2xl transition-all duration-500 overflow-hidden border border-gray-100/50 transform hover:-translate-y-2"
              onMouseEnter={() => setHoveredItem(item.skuCode)}
              onMouseLeave={() => setHoveredItem(null)}
              style={{ animationDelay: `${index * 100}ms` }}
            >
              {/* Heart Badge */}
              <div className="absolute top-4 right-4 z-10">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
                  hoveredItem === item.skuCode 
                    ? 'bg-pink-500 scale-110' 
                    : 'bg-white/90 backdrop-blur-sm shadow-md'
                }`}>
                  <svg 
                    className={`w-5 h-5 transition-colors duration-300 ${
                      hoveredItem === item.skuCode ? 'text-white' : 'text-pink-500'
                    }`} 
                    fill="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                </div>
              </div>

              {/* Image Container */}
              <Link to={`/product/${item.skuCode}`} className="block relative aspect-square overflow-hidden bg-gradient-to-br from-gray-50 to-gray-100">
                <img
                  src={item.imageUrl || 'https://via.placeholder.com/300x300?text=No+Image'}
                  alt={item.name}
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700 ease-out"
                />
                {/* Gradient Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
              </Link>

              {/* Content */}
              <div className="p-5">
                <Link to={`/product/${item.skuCode}`}>
                  <h3 className="font-semibold text-gray-800 mb-2 line-clamp-2 group-hover:text-pink-600 transition-colors duration-300 text-lg">
                    {item.name}
                  </h3>
                </Link>
                
                {/* Price Tag */}
                <div className="flex items-baseline gap-2 mb-5">
                  <span className="text-2xl font-bold bg-gradient-to-r from-pink-600 to-purple-600 bg-clip-text text-transparent">
                    ${item.price?.toFixed(2) || '0.00'}
                  </span>
                </div>

                {/* Actions */}
                <div className="flex gap-3">
                  <button
                    onClick={() => handleMoveToCart(item.skuCode)}
                    disabled={movingToCart === item.skuCode}
                    className="flex-1 flex items-center justify-center gap-2 px-4 py-3 bg-gradient-to-r from-pink-500 to-rose-500 hover:from-pink-600 hover:to-rose-600 disabled:from-pink-300 disabled:to-rose-300 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-all duration-300 transform hover:scale-[1.02] shadow-md hover:shadow-lg"
                  >
                    {movingToCart === item.skuCode ? (
                      <>
                        <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        <span>Moving...</span>
                      </>
                    ) : (
                      <>
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
                        </svg>
                        <span>Add to Cart</span>
                      </>
                    )}
                  </button>
                  <button
                    onClick={() => handleRemoveItem(item.skuCode)}
                    disabled={removingItem === item.skuCode}
                    className="w-12 h-12 flex items-center justify-center text-gray-400 hover:text-red-500 bg-gray-50 hover:bg-red-50 rounded-xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed group/remove"
                    title="Remove from wishlist"
                  >
                    {removingItem === item.skuCode ? (
                      <div className="w-5 h-5 border-2 border-red-400 border-t-transparent rounded-full animate-spin"></div>
                    ) : (
                      <svg className="w-5 h-5 group-hover/remove:scale-110 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              {/* Decorative gradient border on hover */}
              <div className="absolute inset-0 rounded-3xl border-2 border-transparent bg-gradient-to-br from-pink-400/20 to-purple-400/20 opacity-0 group-hover:opacity-100 transition-opacity duration-500 pointer-events-none"></div>
            </div>
          ))}
        </div>
      </div>

      {/* CSS for floating animation */}
      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(100vh) rotate(0deg); opacity: 0; }
          10% { opacity: 0.2; }
          90% { opacity: 0.2; }
          100% { transform: translateY(-100vh) rotate(360deg); opacity: 0; }
        }
        .animate-float {
          animation: float linear infinite;
        }
      `}</style>
    </div>
  );
};

export default Wishlist;
