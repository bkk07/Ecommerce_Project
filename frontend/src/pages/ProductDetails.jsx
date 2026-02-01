import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { fetchProductBySku } from '../api/productApi';
import { addItemToCart, selectAddingItem, selectSuccessMessage, selectCartError, selectCartSkuCodes, clearSuccessMessage, clearError } from '../features/cart/cartSlice';
import { addItemToWishlist, removeItemFromWishlist, selectWishlistSkuCodes, selectWishlistAddingItem, selectWishlistSuccessMessage, selectWishlistError, clearSuccessMessage as clearWishlistSuccess, clearError as clearWishlistError } from '../features/wishlist/wishlistSlice';
import { selectIsAuthenticated } from '../features/auth/authSlice';

const ProductDetails = () => {
  const { sku } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);

  const isAuthenticated = useSelector(selectIsAuthenticated);
  const isAddingToCart = useSelector(selectAddingItem);
  const cartSuccessMessage = useSelector(selectSuccessMessage);
  const cartError = useSelector(selectCartError);
  const cartSkuCodes = useSelector(selectCartSkuCodes);
  
  // Wishlist selectors
  const wishlistSkuCodes = useSelector(selectWishlistSkuCodes);
  const isAddingToWishlist = useSelector(selectWishlistAddingItem);
  const wishlistSuccessMessage = useSelector(selectWishlistSuccessMessage);
  const wishlistError = useSelector(selectWishlistError);
  
  // Check if this product is already in cart
  const isInCart = sku && cartSkuCodes.includes(sku);
  
  // Check if this product is already in wishlist
  const isInWishlist = sku && wishlistSkuCodes.includes(sku);

  useEffect(() => {
    const loadProduct = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchProductBySku(sku);
        setProduct(data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load product details');
      } finally {
        setLoading(false);
      }
    };

    if (sku) {
      loadProduct();
    }
  }, [sku]);

  // Clear messages after some time
  useEffect(() => {
    if (cartSuccessMessage || cartError) {
      const timer = setTimeout(() => {
        dispatch(clearSuccessMessage());
        dispatch(clearError());
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [cartSuccessMessage, cartError, dispatch]);

  // Clear wishlist messages after some time
  useEffect(() => {
    if (wishlistSuccessMessage || wishlistError) {
      const timer = setTimeout(() => {
        dispatch(clearWishlistSuccess());
        dispatch(clearWishlistError());
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [wishlistSuccessMessage, wishlistError, dispatch]);

  const handleQuantityChange = (delta) => {
    setQuantity((prev) => Math.max(1, Math.min(10, prev + delta)));
  };

  const handleAddToCart = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/product/${sku}` } });
      return;
    }
    
    if (isInCart) {
      navigate('/cart');
      return;
    }
    
    // Get the primary image URL from product data
    const images = product?.selectedVariant?.images || [];
    const primaryImage = images.find(img => img.isPrimary) || images[0];
    const imageUrl = primaryImage?.url || '';
    const price = product?.selectedVariant?.price || 0;
    
    dispatch(addItemToCart({ skuCode: sku, quantity, imageUrl, price }));
  };

  const handleBuyNow = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/product/${sku}` } });
      return;
    }

    const images = product?.selectedVariant?.images || [];
    const primaryImage = images.find(img => img.isPrimary) || images[0];
    const imageUrl = primaryImage?.url || '';
    const price = product?.selectedVariant?.price || 0;

    // Create checkout item for direct purchase
    const buyNowItem = {
      skuCode: sku,
      quantity: quantity,
      price: price,
      name: product?.name || '',
      imageUrl: imageUrl
    };

    // Navigate to checkout page with the item
    navigate('/checkout', { 
      state: { 
        buyNowItem: buyNowItem,
        isBuyNow: true
      } 
    });
  };

  const handleWishlist = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/product/${sku}` } });
      return;
    }

    if (isInWishlist) {
      dispatch(removeItemFromWishlist(sku));
    } else {
      const images = product?.selectedVariant?.images || [];
      const primaryImage = images.find(img => img.isPrimary) || images[0];
      const imageUrl = primaryImage?.url || '';
      const price = product?.selectedVariant?.price || 0;

      dispatch(addItemToWishlist({
        skuCode: sku,
        name: product?.name || '',
        price: price,
        imageUrl: imageUrl,
        productId: product?.id || null
      }));
    }
  };

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-gray-600">Loading product details...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-[calc(100vh-200px)] flex items-center justify-center px-4">
        <div className="text-center">
          <div className="mx-auto w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mb-4">
            <svg className="w-10 h-10 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Product Not Found</h2>
          <p className="text-gray-600 mb-6">{error}</p>
          <button
            onClick={() => navigate(-1)}
            className="px-6 py-3 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Go Back
          </button>
        </div>
      </div>
    );
  }

  if (!product) {
    return null;
  }

  const { name, description, brand, selectedVariant } = product;
  const images = selectedVariant?.images || [];
  const price = selectedVariant?.price || 0;
  const specs = selectedVariant?.specs || {};
  const averageRating = selectedVariant?.averageRating || 0;
  const ratingCount = selectedVariant?.ratingCount || 0;
  const hasRating = averageRating > 0 && ratingCount > 0;

  // Generate star rating display
  const renderStars = (rating) => {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;

    for (let i = 0; i < fullStars; i++) {
      stars.push(
        <svg key={`full-${i}`} className="w-5 h-5 text-yellow-400 fill-current" viewBox="0 0 20 20">
          <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    if (hasHalfStar) {
      stars.push(
        <svg key="half" className="w-5 h-5 text-yellow-400" viewBox="0 0 20 20">
          <defs>
            <linearGradient id="halfGradDetail">
              <stop offset="50%" stopColor="currentColor" />
              <stop offset="50%" stopColor="#D1D5DB" />
            </linearGradient>
          </defs>
          <path fill="url(#halfGradDetail)" d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    const emptyStars = 5 - Math.ceil(rating);
    for (let i = 0; i < emptyStars; i++) {
      stars.push(
        <svg key={`empty-${i}`} className="w-5 h-5 text-gray-300 fill-current" viewBox="0 0 20 20">
          <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
        </svg>
      );
    }

    return stars;
  };

  // Get primary image or first image
  const primaryImageIndex = images.findIndex((img) => img.isPrimary);
  const displayImages = images.length > 0 ? images : [{ url: 'https://via.placeholder.com/500x500?text=No+Image', isPrimary: true }];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center space-x-2 text-sm text-gray-500 mb-8">
        <Link to="/" className="hover:text-indigo-600 transition-colors">Home</Link>
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
        <Link to="/search" className="hover:text-indigo-600 transition-colors">Products</Link>
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
        <span className="text-gray-900 font-medium truncate max-w-[200px]">{name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        {/* Image Gallery */}
        <div className="space-y-4">
          {/* Main Image */}
          <div className="aspect-square bg-gray-100 rounded-2xl overflow-hidden">
            <img
              src={displayImages[selectedImage]?.url}
              alt={name}
              className="w-full h-full object-cover"
            />
          </div>

          {/* Thumbnail Gallery */}
          {displayImages.length > 1 && (
            <div className="flex space-x-3 overflow-x-auto pb-2">
              {displayImages.map((image, index) => (
                <button
                  key={index}
                  onClick={() => setSelectedImage(index)}
                  className={`flex-shrink-0 w-20 h-20 rounded-lg overflow-hidden border-2 transition-all ${
                    selectedImage === index
                      ? 'border-indigo-600 ring-2 ring-indigo-200'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <img
                    src={image.url}
                    alt={`${name} - ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          {/* Brand */}
          {brand && (
            <p className="text-sm font-medium text-indigo-600 uppercase tracking-wide">
              {brand}
            </p>
          )}

          {/* Name */}
          <h1 className="text-3xl font-bold text-gray-900">{name}</h1>

          {/* SKU */}
          <p className="text-sm text-gray-500">
            SKU: <span className="font-mono">{selectedVariant?.sku}</span>
          </p>

          {/* Price */}
          <div className="flex items-baseline space-x-3">
            <span className="text-4xl font-bold text-indigo-600">
              ${typeof price === 'number' ? price.toFixed(2) : price}
            </span>
          </div>

          {/* Rating */}
          <div className="flex items-center space-x-2">
            {hasRating ? (
              <>
                <div className="flex">{renderStars(averageRating)}</div>
                <span className="text-lg font-medium text-gray-900">{averageRating.toFixed(1)}</span>
                <span className="text-gray-500">({ratingCount} {ratingCount === 1 ? 'review' : 'reviews'})</span>
              </>
            ) : (
              <span className="text-gray-400 italic">Not yet rated</span>
            )}
          </div>

          {/* Description */}
          {description && (
            <div className="prose prose-sm text-gray-600">
              <p>{description}</p>
            </div>
          )}

          {/* Specifications */}
          {Object.keys(specs).length > 0 && (
            <div className="border-t border-gray-200 pt-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Specifications</h3>
              <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {Object.entries(specs).map(([key, value]) => (
                  <div key={key} className="bg-gray-50 rounded-lg px-4 py-3">
                    <dt className="text-sm text-gray-500 capitalize">{key.replace(/_/g, ' ')}</dt>
                    <dd className="text-sm font-medium text-gray-900 mt-1">{String(value)}</dd>
                  </div>
                ))}
              </dl>
            </div>
          )}

          {/* Quantity & Add to Cart */}
          <div className="border-t border-gray-200 pt-6 space-y-4">
            {/* Success Message */}
            {cartSuccessMessage && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-lg flex items-center space-x-2 text-green-700">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span>{cartSuccessMessage}</span>
              </div>
            )}

            {/* Error Message */}
            {cartError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center space-x-2 text-red-700">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>{cartError}</span>
              </div>
            )}

            {/* Quantity Selector */}
            <div className="flex items-center space-x-4">
              <span className="text-sm font-medium text-gray-700">Quantity:</span>
              <div className="flex items-center border border-gray-300 rounded-lg">
                <button
                  onClick={() => handleQuantityChange(-1)}
                  className="px-4 py-2 text-gray-600 hover:bg-gray-100 transition-colors rounded-l-lg"
                  disabled={quantity <= 1}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
                  </svg>
                </button>
                <span className="px-4 py-2 text-gray-900 font-medium min-w-[50px] text-center">
                  {quantity}
                </span>
                <button
                  onClick={() => handleQuantityChange(1)}
                  className="px-4 py-2 text-gray-600 hover:bg-gray-100 transition-colors rounded-r-lg"
                  disabled={quantity >= 10}
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                  </svg>
                </button>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col sm:flex-row gap-4">
              {isInCart ? (
                <button
                  onClick={() => navigate('/cart')}
                  className="flex-1 flex items-center justify-center space-x-2 px-8 py-4 bg-green-100 text-green-700 font-semibold rounded-xl cursor-default"
                  disabled
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>In Cart</span>
                </button>
              ) : (
                <button
                  onClick={handleAddToCart}
                  disabled={isAddingToCart}
                  className="flex-1 flex items-center justify-center space-x-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-colors"
                >
                  {isAddingToCart ? (
                    <>
                      <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Adding...</span>
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
              )}
              {/* Buy Now Button */}
              <button
                onClick={handleBuyNow}
                className="flex-1 flex items-center justify-center space-x-2 px-8 py-4 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-xl transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                <span>Buy Now</span>
              </button>
              <button
                onClick={handleWishlist}
                disabled={isAddingToWishlist}
                className={`flex items-center justify-center px-6 py-4 border-2 font-medium rounded-xl transition-colors ${
                  isInWishlist 
                    ? 'border-red-500 text-red-500 hover:border-red-600 hover:text-red-600' 
                    : 'border-gray-300 hover:border-indigo-600 text-gray-700 hover:text-indigo-600'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {isAddingToWishlist ? (
                  <div className="w-5 h-5 border-2 border-current border-t-transparent rounded-full animate-spin"></div>
                ) : (
                  <svg className="w-5 h-5" fill={isInWishlist ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                )}
              </button>
            </div>
          </div>

          {/* Additional Info */}
          <div className="border-t border-gray-200 pt-6">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="flex items-center space-x-3 text-sm text-gray-600">
                <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span>Free Shipping</span>
              </div>
              <div className="flex items-center space-x-3 text-sm text-gray-600">
                <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
                <span>Secure Payment</span>
              </div>
              <div className="flex items-center space-x-3 text-sm text-gray-600">
                <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                <span>Easy Returns</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProductDetails;
