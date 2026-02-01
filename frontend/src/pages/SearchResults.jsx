import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import ProductCard from '../components/ProductCard';
import {
  searchProductsFiltered,
  selectSearchResults,
  selectSearchStatus,
  selectSearchError,
  selectPagination,
  selectCategoryFacets,
  clearFilters,
} from '../features/products/productSlice';

const SORT_OPTIONS = [
  { value: '', label: 'Relevance' },
  { value: 'price-asc', label: 'Price: Low to High' },
  { value: 'price-desc', label: 'Price: High to Low' },
  { value: 'name-asc', label: 'Name: A to Z' },
  { value: 'name-desc', label: 'Name: Z to A' },
  { value: 'averageRating-desc', label: 'Highest Rated' },
];

const RATING_FILTERS = [
  { value: '', label: 'All Ratings' },
  { value: '4', label: '4★ & Above' },
  { value: '3', label: '3★ & Above' },
  { value: '2', label: '2★ & Above' },
  { value: '1', label: '1★ & Above' },
];

const CATEGORIES = [
  'Electronics',
  'Fashion',
  'Home & Living',
  'Sports',
  'Beauty',
  'Smartphones',
  'Laptops',
  'Accessories',
];

const PRICE_RANGES = [
  { label: 'Under $50', min: 0, max: 50 },
  { label: '$50 - $100', min: 50, max: 100 },
  { label: '$100 - $250', min: 100, max: 250 },
  { label: '$250 - $500', min: 250, max: 500 },
  { label: '$500 - $1000', min: 500, max: 1000 },
  { label: 'Over $1000', min: 1000, max: '' },
];

const SearchResults = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('q') || '';
  const dispatch = useDispatch();

  const products = useSelector(selectSearchResults);
  const status = useSelector(selectSearchStatus);
  const error = useSelector(selectSearchError);
  const pagination = useSelector(selectPagination);
  const categoryFacets = useSelector(selectCategoryFacets);

  // Local filter state
  const [filters, setFilters] = useState({
    category: searchParams.get('category') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || '',
    minRating: searchParams.get('minRating') || '',
    sortBy: searchParams.get('sortBy') || '',
    sortOrder: searchParams.get('sortOrder') || 'asc',
  });

  const [showMobileFilters, setShowMobileFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 12;

  // Fetch products with current filters
  const fetchWithFilters = useCallback(() => {
    const [sortField, sortDirection] = filters.sortBy ? filters.sortBy.split('-') : ['', 'asc'];
    
    dispatch(searchProductsFiltered({
      keyword,
      category: filters.category,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      minRating: filters.minRating,
      sortBy: sortField,
      sortOrder: sortDirection,
      page: currentPage,
      size: pageSize,
    }));
  }, [dispatch, keyword, filters, currentPage]);

  // Update URL with filters
  const updateURLParams = useCallback(() => {
    const params = new URLSearchParams();
    if (keyword) params.set('q', keyword);
    if (filters.category) params.set('category', filters.category);
    if (filters.minPrice) params.set('minPrice', filters.minPrice);
    if (filters.maxPrice) params.set('maxPrice', filters.maxPrice);
    if (filters.minRating) params.set('minRating', filters.minRating);
    if (filters.sortBy) params.set('sortBy', filters.sortBy);
    setSearchParams(params, { replace: true });
  }, [keyword, filters, setSearchParams]);

  // Initial fetch and when filters change
  useEffect(() => {
    if (keyword) {
      fetchWithFilters();
      updateURLParams();
    }
  }, [keyword, filters, currentPage, fetchWithFilters, updateURLParams]);

  // Handle filter changes
  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setCurrentPage(0); // Reset to first page on filter change
  };

  // Handle price range selection
  const handlePriceRangeSelect = (min, max) => {
    setFilters(prev => ({
      ...prev,
      minPrice: min.toString(),
      maxPrice: max.toString(),
    }));
    setCurrentPage(0);
  };

  // Clear all filters
  const handleClearFilters = () => {
    setFilters({
      category: '',
      minPrice: '',
      maxPrice: '',
      minRating: '',
      sortBy: '',
      sortOrder: 'asc',
    });
    setCurrentPage(0);
    dispatch(clearFilters());
  };

  // Check if any filter is active
  const hasActiveFilters = filters.category || filters.minPrice || filters.maxPrice || filters.minRating || filters.sortBy;

  // Get active filter count
  const activeFilterCount = [
    filters.category,
    filters.minPrice || filters.maxPrice,
    filters.minRating,
    filters.sortBy,
  ].filter(Boolean).length;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Breadcrumb */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <nav className="flex items-center space-x-2 text-sm">
            <Link to="/" className="text-gray-500 hover:text-indigo-600 transition-colors">
              Home
            </Link>
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
            <span className="text-gray-900 font-medium">Search Results</span>
          </nav>
        </div>
      </div>

      {/* Page Header */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 py-8 md:py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 className="text-2xl md:text-4xl font-bold text-white mb-2">
            Search Results
          </h1>
          <p className="text-white/80 text-lg">
            {keyword ? (
              <>Showing results for "<span className="font-semibold">{keyword}</span>"</>
            ) : (
              'Enter a search term to find products'
            )}
          </p>
          {status === 'succeeded' && (
            <p className="text-white/70 text-sm mt-1">
              {pagination?.totalElements || products.length} products found
            </p>
          )}
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Mobile Filter Button */}
          <div className="lg:hidden">
            <button
              onClick={() => setShowMobileFilters(true)}
              className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-white border border-gray-200 rounded-lg shadow-sm hover:bg-gray-50 transition-colors"
            >
              <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
              </svg>
              <span className="font-medium">Filters & Sort</span>
              {activeFilterCount > 0 && (
                <span className="bg-indigo-600 text-white text-xs px-2 py-0.5 rounded-full">
                  {activeFilterCount}
                </span>
              )}
            </button>
          </div>

          {/* Filter Sidebar - Desktop */}
          <aside className="hidden lg:block w-64 flex-shrink-0">
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-5 sticky top-24">
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-lg font-semibold text-gray-900">Filters</h2>
                {hasActiveFilters && (
                  <button
                    onClick={handleClearFilters}
                    className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    Clear all
                  </button>
                )}
              </div>

              {/* Sort By */}
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-gray-900 mb-3">Sort By</h3>
                <select
                  value={filters.sortBy}
                  onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm"
                >
                  {SORT_OPTIONS.map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Category Filter */}
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-gray-900 mb-3">Category</h3>
                <div className="space-y-2 max-h-48 overflow-y-auto">
                  {CATEGORIES.map(category => (
                    <label key={category} className="flex items-center gap-2 cursor-pointer group">
                      <input
                        type="radio"
                        name="category"
                        checked={filters.category === category}
                        onChange={() => handleFilterChange('category', category)}
                        className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span className="text-sm text-gray-700 group-hover:text-indigo-600 transition-colors">
                        {category}
                        {categoryFacets[category] && (
                          <span className="text-gray-400 ml-1">({categoryFacets[category]})</span>
                        )}
                      </span>
                    </label>
                  ))}
                  {filters.category && (
                    <button
                      onClick={() => handleFilterChange('category', '')}
                      className="text-xs text-gray-500 hover:text-indigo-600 mt-1"
                    >
                      Clear category
                    </button>
                  )}
                </div>
              </div>

              {/* Price Range Filter */}
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-gray-900 mb-3">Price Range</h3>
                <div className="space-y-2">
                  {PRICE_RANGES.map((range, idx) => (
                    <label key={idx} className="flex items-center gap-2 cursor-pointer group">
                      <input
                        type="radio"
                        name="priceRange"
                        checked={filters.minPrice === range.min.toString() && filters.maxPrice === range.max.toString()}
                        onChange={() => handlePriceRangeSelect(range.min, range.max)}
                        className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span className="text-sm text-gray-700 group-hover:text-indigo-600 transition-colors">
                        {range.label}
                      </span>
                    </label>
                  ))}
                </div>

                {/* Custom Price Range */}
                <div className="mt-4 pt-4 border-t border-gray-100">
                  <p className="text-xs text-gray-500 mb-2">Custom Range</p>
                  <div className="flex items-center gap-2">
                    <input
                      type="number"
                      placeholder="Min"
                      value={filters.minPrice}
                      onChange={(e) => handleFilterChange('minPrice', e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                    <span className="text-gray-400">-</span>
                    <input
                      type="number"
                      placeholder="Max"
                      value={filters.maxPrice}
                      onChange={(e) => handleFilterChange('maxPrice', e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>
              </div>

              {/* Rating Filter */}
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-gray-900 mb-3">Customer Rating</h3>
                <div className="space-y-2">
                  {RATING_FILTERS.map((option) => (
                    <label key={option.value} className="flex items-center gap-2 cursor-pointer group">
                      <input
                        type="radio"
                        name="ratingFilter"
                        checked={filters.minRating === option.value}
                        onChange={() => handleFilterChange('minRating', option.value)}
                        className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span className="text-sm text-gray-700 group-hover:text-indigo-600 transition-colors flex items-center gap-1">
                        {option.value ? (
                          <>
                            <span className="text-yellow-400">{'★'.repeat(parseInt(option.value))}</span>
                            <span className="text-gray-300">{'★'.repeat(5 - parseInt(option.value))}</span>
                            <span className="ml-1">& Up</span>
                          </>
                        ) : (
                          option.label
                        )}
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Active Filters Summary */}
              {hasActiveFilters && (
                <div className="pt-4 border-t border-gray-100">
                  <p className="text-xs text-gray-500 mb-2">Active Filters:</p>
                  <div className="flex flex-wrap gap-2">
                    {filters.category && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs">
                        {filters.category}
                        <button onClick={() => handleFilterChange('category', '')} className="hover:text-indigo-900">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </span>
                    )}
                    {(filters.minPrice || filters.maxPrice) && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs">
                        ${filters.minPrice || '0'} - ${filters.maxPrice || '∞'}
                        <button onClick={() => { handleFilterChange('minPrice', ''); handleFilterChange('maxPrice', ''); }} className="hover:text-indigo-900">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </span>
                    )}
                    {filters.sortBy && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs">
                        {SORT_OPTIONS.find(o => o.value === filters.sortBy)?.label}
                        <button onClick={() => handleFilterChange('sortBy', '')} className="hover:text-indigo-900">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </span>
                    )}
                    {filters.minRating && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs">
                        {filters.minRating}★ & Up
                        <button onClick={() => handleFilterChange('minRating', '')} className="hover:text-indigo-900">
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                          </svg>
                        </button>
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </aside>

          {/* Mobile Filter Sidebar */}
          {showMobileFilters && (
            <div className="fixed inset-0 z-50 lg:hidden">
              <div className="absolute inset-0 bg-black/50" onClick={() => setShowMobileFilters(false)} />
              <div className="absolute right-0 top-0 bottom-0 w-full max-w-sm bg-white shadow-xl overflow-y-auto">
                <div className="p-5">
                  <div className="flex items-center justify-between mb-5">
                    <h2 className="text-lg font-semibold text-gray-900">Filters & Sort</h2>
                    <button
                      onClick={() => setShowMobileFilters(false)}
                      className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                      <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>

                  {/* Sort By */}
                  <div className="mb-6">
                    <h3 className="text-sm font-semibold text-gray-900 mb-3">Sort By</h3>
                    <select
                      value={filters.sortBy}
                      onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                      className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                    >
                      {SORT_OPTIONS.map(option => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Category Filter */}
                  <div className="mb-6">
                    <h3 className="text-sm font-semibold text-gray-900 mb-3">Category</h3>
                    <div className="space-y-2">
                      {CATEGORIES.map(category => (
                        <label key={category} className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="radio"
                            name="category-mobile"
                            checked={filters.category === category}
                            onChange={() => handleFilterChange('category', category)}
                            className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                          />
                          <span className="text-sm text-gray-700">{category}</span>
                        </label>
                      ))}
                    </div>
                  </div>

                  {/* Price Range Filter */}
                  <div className="mb-6">
                    <h3 className="text-sm font-semibold text-gray-900 mb-3">Price Range</h3>
                    <div className="space-y-2">
                      {PRICE_RANGES.map((range, idx) => (
                        <label key={idx} className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="radio"
                            name="priceRange-mobile"
                            checked={filters.minPrice === range.min.toString() && filters.maxPrice === range.max.toString()}
                            onChange={() => handlePriceRangeSelect(range.min, range.max)}
                            className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                          />
                          <span className="text-sm text-gray-700">{range.label}</span>
                        </label>
                      ))}
                    </div>

                    <div className="mt-4 pt-4 border-t border-gray-100">
                      <p className="text-xs text-gray-500 mb-2">Custom Range</p>
                      <div className="flex items-center gap-2">
                        <input
                          type="number"
                          placeholder="Min"
                          value={filters.minPrice}
                          onChange={(e) => handleFilterChange('minPrice', e.target.value)}
                          className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                        <span className="text-gray-400">-</span>
                        <input
                          type="number"
                          placeholder="Max"
                          value={filters.maxPrice}
                          onChange={(e) => handleFilterChange('maxPrice', e.target.value)}
                          className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Rating Filter (Mobile) */}
                  <div className="mb-6">
                    <h3 className="text-sm font-semibold text-gray-900 mb-3">Customer Rating</h3>
                    <div className="space-y-2">
                      {RATING_FILTERS.map((option) => (
                        <label key={option.value} className="flex items-center gap-2 cursor-pointer">
                          <input
                            type="radio"
                            name="ratingFilter-mobile"
                            checked={filters.minRating === option.value}
                            onChange={() => handleFilterChange('minRating', option.value)}
                            className="w-4 h-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                          />
                          <span className="text-sm text-gray-700 flex items-center gap-1">
                            {option.value ? (
                              <>
                                <span className="text-yellow-400">{'★'.repeat(parseInt(option.value))}</span>
                                <span className="text-gray-300">{'★'.repeat(5 - parseInt(option.value))}</span>
                                <span className="ml-1">& Up</span>
                              </>
                            ) : (
                              option.label
                            )}
                          </span>
                        </label>
                      ))}
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-3 pt-4 border-t border-gray-100">
                    <button
                      onClick={handleClearFilters}
                      className="flex-1 px-4 py-2 border border-gray-200 rounded-lg text-gray-700 font-medium hover:bg-gray-50 transition-colors"
                    >
                      Clear All
                    </button>
                    <button
                      onClick={() => setShowMobileFilters(false)}
                      className="flex-1 px-4 py-2 bg-indigo-600 text-white rounded-lg font-medium hover:bg-indigo-700 transition-colors"
                    >
                      Apply Filters
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Products Section */}
          <main className="flex-1">
            {/* Results Header */}
            <div className="hidden lg:flex items-center justify-between mb-6">
              <p className="text-gray-600">
                {status === 'succeeded' && (
                  <>
                    Showing <span className="font-semibold text-gray-900">{products.length}</span> of{' '}
                    <span className="font-semibold text-gray-900">{pagination?.totalElements || products.length}</span> products
                  </>
                )}
              </p>
              <div className="flex items-center gap-4">
                <select
                  value={filters.sortBy}
                  onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                  className="px-4 py-2 bg-white border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm"
                >
                  {SORT_OPTIONS.map(option => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Loading State */}
            {status === 'loading' && (
              <div className="flex justify-center items-center py-24">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
                  <p className="text-gray-600">Searching for "{keyword}"...</p>
                </div>
              </div>
            )}

            {/* Error State */}
            {status === 'failed' && (
              <div className="text-center py-24 bg-white rounded-xl">
                <div className="text-red-500 mb-4">
                  <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Search failed</h3>
                <p className="text-gray-600 mb-4">{error}</p>
                <button
                  onClick={() => fetchWithFilters()}
                  className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                >
                  Try Again
                </button>
              </div>
            )}

            {/* Products Grid */}
            {status === 'succeeded' && products.length > 0 && (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
                  {products.map((product, index) => (
                    <ProductCard key={product.skuCode || product.id || index} product={product} />
                  ))}
                </div>

                {/* Pagination */}
                {pagination && pagination.totalPages > 1 && (
                  <div className="flex items-center justify-center mt-8 gap-2">
                    <button
                      onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                      disabled={currentPage === 0}
                      className="px-4 py-2 bg-white border border-gray-200 rounded-lg text-gray-700 font-medium hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      Previous
                    </button>
                    
                    <div className="flex items-center gap-1">
                      {[...Array(Math.min(5, pagination.totalPages))].map((_, idx) => {
                        let pageNum;
                        if (pagination.totalPages <= 5) {
                          pageNum = idx;
                        } else if (currentPage < 3) {
                          pageNum = idx;
                        } else if (currentPage > pagination.totalPages - 4) {
                          pageNum = pagination.totalPages - 5 + idx;
                        } else {
                          pageNum = currentPage - 2 + idx;
                        }

                        return (
                          <button
                            key={pageNum}
                            onClick={() => setCurrentPage(pageNum)}
                            className={`w-10 h-10 rounded-lg font-medium transition-colors ${
                              currentPage === pageNum
                                ? 'bg-indigo-600 text-white'
                                : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'
                            }`}
                          >
                            {pageNum + 1}
                          </button>
                        );
                      })}
                    </div>

                    <button
                      onClick={() => setCurrentPage(prev => Math.min(pagination.totalPages - 1, prev + 1))}
                      disabled={currentPage === pagination.totalPages - 1}
                      className="px-4 py-2 bg-white border border-gray-200 rounded-lg text-gray-700 font-medium hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      Next
                    </button>
                  </div>
                )}
              </>
            )}

            {/* Empty State */}
            {status === 'succeeded' && products.length === 0 && (
              <div className="text-center py-24 bg-white rounded-xl">
                <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No products found</h3>
                <p className="text-gray-600 mb-4">
                  {hasActiveFilters
                    ? 'Try adjusting your filters or search terms'
                    : `We couldn't find any products matching "${keyword}".`}
                </p>
                {hasActiveFilters && (
                  <button
                    onClick={handleClearFilters}
                    className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors mb-4"
                  >
                    Clear Filters
                  </button>
                )}
                <div className="space-y-2">
                  <p className="text-sm text-gray-500">Try:</p>
                  <ul className="text-sm text-gray-500 list-disc list-inside">
                    <li>Checking your spelling</li>
                    <li>Using more general terms</li>
                    <li>Removing some filters</li>
                    <li>Searching for a different product</li>
                  </ul>
                </div>
                <Link
                  to="/"
                  className="inline-block mt-6 px-6 py-2 border border-indigo-600 text-indigo-600 rounded-lg hover:bg-indigo-50 transition-colors"
                >
                  Back to Home
                </Link>
              </div>
            )}

            {/* No Keyword State */}
            {status === 'idle' && !keyword && (
              <div className="text-center py-24 bg-white rounded-xl">
                <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Start searching</h3>
                <p className="text-gray-600 mb-4">
                  Use the search bar above to find products.
                </p>
                <Link
                  to="/"
                  className="inline-block px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                >
                  Back to Home
                </Link>
              </div>
            )}
          </main>
        </div>
      </div>
    </div>
  );
};

export default SearchResults;
