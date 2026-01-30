import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { addItemToCart, selectCartSkuCodes, selectAddingItem } from '../features/cart/cartSlice';
import { selectIsAuthenticated } from '../features/auth/authSlice';

const ProductCard = ({ product }) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const cartSkuCodes = useSelector(selectCartSkuCodes);
  const isAddingItem = useSelector(selectAddingItem);
  
  const {
    name,
    description,
    price,
    imageUrl,
    category,
    sku,
    skuCode, // Alternative field name from search results
    rating = 4.5,
    reviewCount = 128,
  } = product;

  // Use skuCode if sku is not available
  const productSku = sku || skuCode;
  
  // Check if this product is already in the cart
  const isInCart = productSku && cartSkuCodes.includes(productSku);

  const handleCardClick = () => {
    if (productSku) {
      navigate(`/product/${productSku}`);
    } else {
      console.log('No SKU found for product:', product);
    }
  };

  const handleAddToCart = (e) => {
    e.stopPropagation(); // Prevent card click
    
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    
    if (isInCart) {
      // Already in cart, navigate to cart
      navigate('/cart');
      return;
    }
    
    dispatch(addItemToCart({ 
      skuCode: productSku, 
      quantity: 1, 
      imageUrl: imageUrl || '',
      price: price || 0
    }));
  };

  const handleBuyNow = (e) => {
    e.stopPropagation(); // Prevent card click
    
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }

    // Create checkout item for direct purchase
    const buyNowItem = {
      skuCode: productSku,
      quantity: 1,
      price: price || 0,
      name: name || '',
      imageUrl: imageUrl || ''
    };

    // Navigate to checkout page with the item
    navigate('/checkout', { 
      state: { 
        buyNowItem: buyNowItem,
        isBuyNow: true
      } 
    });
  };

  const handleWishlist = (e) => {
    e.stopPropagation(); // Prevent card click
    // TODO: Implement wishlist functionality
    console.log('Add to wishlist:', product);
  };

  // Generate star rating display
  const renderStars = (rating) => {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;

    for (let i = 0; i < fullStars; i++) {
      stars.push(
        <svg key={`full-${i}`} className="w-4 h-4 text-yellow-400 fill-current" viewBox="0 0 20 20">
          <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    if (hasHalfStar) {
      stars.push(
        <svg key="half" className="w-4 h-4 text-yellow-400" viewBox="0 0 20 20">
          <defs>
            <linearGradient id="halfGrad">
              <stop offset="50%" stopColor="currentColor" />
              <stop offset="50%" stopColor="#D1D5DB" />
            </linearGradient>
          </defs>
          <path fill="url(#halfGrad)" d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    const emptyStars = 5 - Math.ceil(rating);
    for (let i = 0; i < emptyStars; i++) {
      stars.push(
        <svg key={`empty-${i}`} className="w-4 h-4 text-gray-300 fill-current" viewBox="0 0 20 20">
          <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    return stars;
  };

  return (
    <div 
      onClick={handleCardClick}
      className="group bg-white rounded-xl shadow-sm hover:shadow-xl transition-all duration-300 overflow-hidden border border-gray-100 cursor-pointer"
    >
      {/* Image Container */}
      <div className="relative overflow-hidden bg-gray-100">
        <img
          src={imageUrl || 'https://via.placeholder.com/300x200?text=Product'}
          alt={name}
          className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
        />
        {/* Category Badge */}
        <span className="absolute top-3 left-3 px-3 py-1 bg-indigo-600 text-white text-xs font-semibold rounded-full">
          {category || 'General'}
        </span>
        {/* Wishlist Button */}
        <button 
          onClick={handleWishlist}
          className="absolute top-3 right-3 p-2 bg-white/80 hover:bg-white rounded-full shadow-sm opacity-0 group-hover:opacity-100 transition-opacity duration-300"
        >
          <svg className="w-5 h-5 text-gray-600 hover:text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
        </button>
      </div>

      {/* Content */}
      <div className="p-4">
        {/* Rating */}
        <div className="flex items-center space-x-1 mb-2">
          <div className="flex">{renderStars(rating)}</div>
          <span className="text-sm text-gray-500 ml-1">({reviewCount})</span>
        </div>

        {/* Product Name */}
        <h3 className="text-lg font-semibold text-gray-800 mb-1 line-clamp-1 group-hover:text-indigo-600 transition-colors">
          {name}
        </h3>

        {/* Description */}
        <p className="text-sm text-gray-500 mb-3 line-clamp-2">
          {description || 'No description available'}
        </p>

        {/* Price and Action Buttons */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xl font-bold text-indigo-600">
              ${typeof price === 'number' ? price.toFixed(2) : price}
            </span>
            {isInCart ? (
              <button 
                onClick={handleAddToCart}
                className="flex items-center space-x-1 px-3 py-1.5 bg-green-100 text-green-700 text-sm font-medium rounded-lg cursor-default"
                disabled
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span>In Cart</span>
              </button>
            ) : (
              <button 
                onClick={handleAddToCart}
                disabled={isAddingItem}
                className="flex items-center space-x-1 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white text-sm font-medium rounded-lg transition-colors duration-200"
              >
                {isAddingItem ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Adding...</span>
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    <span>Cart</span>
                  </>
                )}
              </button>
            )}
          </div>
          {/* Buy Now Button */}
          <button 
            onClick={handleBuyNow}
            className="w-full flex items-center justify-center space-x-1 px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white text-sm font-medium rounded-lg transition-colors duration-200"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <span>Buy Now</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
