import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import ProductCard from '../components/ProductCard';
import electronicsCategoryImage from '../assets/electronics_category.jpg';
import fashionCategoryImage from '../assets/fashion_category.jpg';
import homeAndLivingCategoryImage from '../assets/HomeAndLiving.jpg';
import sportsCategoryImage from '../assets/sports_category.jpg';
import beautyCategoryImage from '../assets/Beauty_Category.jpg';
import {
  fetchProducts,
  selectAllProducts,
  selectProductsStatus,
  selectProductsError,
} from '../features/products/productSlice';

const categories = [
  {
    name: 'Electronics',
    icon: '📱',
    description: 'Phones, Laptops & More',
    gradient: 'from-blue-500 to-indigo-600',
  },
  {
    name: 'Fashion',
    icon: '👗',
    description: 'Clothing & Accessories',
    gradient: 'from-pink-500 to-rose-600',
  },
  {
    name: 'Home & Living',
    icon: '🏠',
    description: 'Furniture & Decor',
    gradient: 'from-green-500 to-emerald-600',
  },
  {
    name: 'Sports',
    icon: '⚽',
    description: 'Equipment & Apparel',
    gradient: 'from-orange-500 to-amber-600',
  },
  {
    name: 'Beauty',
    icon: '💄',
    description: 'Skincare & Makeup',
    gradient: 'from-purple-500 to-violet-600',
  },
];

const heroSlides = [
  {
    title: 'Style Meets Street',
    subtitle: 'Fresh fashion drops, curated picks, and daily deals built for modern shoppers.',
    ctaLabel: 'Shop Fashion',
    ctaLink: '/products/Fashion',
    image:
      'https://images.pexels.com/photos/10526287/pexels-photo-10526287.jpeg?cs=srgb&dl=pexels-mavluda-tashbaeva-133603941-10526287.jpg&fm=jpg',
  },
  {
    title: 'Upgrade Your Tech',
    subtitle: 'Discover smartphones, laptops, and smart accessories from top brands.',
    ctaLabel: 'Browse Electronics',
    ctaLink: '/products/Electronics',
    image:
      'https://images.pexels.com/photos/4506937/pexels-photo-4506937.jpeg?cs=srgb&dl=pexels-chelsey-horne-2961698-4506937.jpg&fm=jpg',
  },
  {
    title: 'Home, Reimagined',
    subtitle: 'Bring comfort and character to every room with elegant home essentials.',
    ctaLabel: 'Explore Home & Living',
    ctaLink: '/products/Home & Living',
    image:
      'https://images.pexels.com/photos/19028887/pexels-photo-19028887.jpeg?cs=srgb&dl=pexels-alejandro-pacheco-162150400-19028887.jpg&fm=jpg',
  },
];

const categoryVisuals = {
  Electronics: {
    badge: 'Top Tech',
    gradient: 'from-sky-500/35 via-blue-700/20 to-slate-950/90',
    accentText: 'text-sky-800',
    badgeShell: 'bg-sky-100',
    iconShell: 'bg-sky-400/25',
    iconColor: 'text-sky-100',
    imagePosition: 'object-center',
    image: electronicsCategoryImage,
    iconPath:
      'M12 18h.01M8 21h8a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2Z',
  },
  Fashion: {
    badge: 'Hot Picks',
    gradient: 'from-rose-500/35 via-pink-700/20 to-slate-950/90',
    accentText: 'text-rose-800',
    badgeShell: 'bg-rose-100',
    iconShell: 'bg-rose-400/25',
    iconColor: 'text-rose-100',
    imagePosition: 'object-center',
    image: fashionCategoryImage,
    iconPath:
      'm7 17 5-5 5 5M5 7h14M7 7V5a2 2 0 1 1 4 0v2m2 0V5a2 2 0 1 1 4 0v2',
  },
  'Home & Living': {
    badge: 'Cozy Space',
    gradient: 'from-emerald-500/35 via-green-700/20 to-slate-950/90',
    accentText: 'text-emerald-800',
    badgeShell: 'bg-emerald-100',
    iconShell: 'bg-emerald-400/25',
    iconColor: 'text-emerald-100',
    imagePosition: 'object-center',
    image: homeAndLivingCategoryImage,
    iconPath: 'm3 11 9-8 9 8M5 10v10h14V10M9 20v-6h6v6',
  },
  Sports: {
    badge: 'Active Life',
    gradient: 'from-orange-500/35 via-amber-700/20 to-slate-950/90',
    accentText: 'text-orange-800',
    badgeShell: 'bg-orange-100',
    iconShell: 'bg-orange-400/25',
    iconColor: 'text-orange-100',
    imagePosition: 'object-center',
    image: sportsCategoryImage,
    iconPath:
      'M8.25 6.75a3.75 3.75 0 1 1 7.5 0v.75h.75a3 3 0 0 1 3 3V14.5a5.5 5.5 0 0 1-5.5 5.5h-4A5.5 5.5 0 0 1 4.5 14.5v-3a3 3 0 0 1 3-3h.75v-.75Z',
  },
  Beauty: {
    badge: 'Glow Up',
    gradient: 'from-fuchsia-500/35 via-pink-700/20 to-slate-950/90',
    accentText: 'text-fuchsia-800',
    badgeShell: 'bg-fuchsia-100',
    iconShell: 'bg-fuchsia-400/25',
    iconColor: 'text-fuchsia-100',
    imagePosition: 'object-center',
    image: beautyCategoryImage,
    iconPath:
      'M9 3h6M10 3v3m4-3v3M6 9h12l-1 9a3 3 0 0 1-3 2H10a3 3 0 0 1-3-2L6 9Z',
  },
};

const categoryShowcase = categories.map((category) => ({
  ...category,
  slug: category.name,
  ...categoryVisuals[category.name],
}));

const Home = () => {
  const dispatch = useDispatch();
  const products = useSelector(selectAllProducts);
  const status = useSelector(selectProductsStatus);
  const error = useSelector(selectProductsError);
  const [activeSlide, setActiveSlide] = useState(0);

  useEffect(() => {
    if (status === 'idle') {
      dispatch(fetchProducts('Smartphones'));
    }
  }, [status, dispatch]);

  useEffect(() => {
    const timer = setInterval(() => {
      setActiveSlide((prev) => (prev + 1) % heroSlides.length);
    }, 5000);

    return () => clearInterval(timer);
  }, []);

  const goToSlide = (index) => setActiveSlide(index);
  const goToPreviousSlide = () =>
    setActiveSlide((prev) => (prev - 1 + heroSlides.length) % heroSlides.length);
  const goToNextSlide = () => setActiveSlide((prev) => (prev + 1) % heroSlides.length);

  const renderSharedProductCards = (keyPrefix = '') => (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
      {products.map((product, index) => (
        <ProductCard key={`${keyPrefix}${product.id || index}`} product={product} />
      ))}
    </div>
  );

  const renderProductGrid = (title, subtitle) => (
    <section className="py-16 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      <div className="text-center mb-12">
        <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">{title}</h2>
        <p className="text-lg text-gray-600 max-w-2xl mx-auto">{subtitle}</p>
      </div>

      {status === 'loading' && (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
        </div>
      )}

      {status === 'failed' && (
        <div className="text-center py-12">
          <div className="text-red-500 mb-4">
            <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <p className="text-gray-600">Failed to load products. Please try again later.</p>
          <p className="text-sm text-gray-400 mt-2">{error}</p>
        </div>
      )}

      {status === 'succeeded' && renderSharedProductCards()}
    </section>
  );

  const renderFlashDealsSection = () => (
    <section className="relative overflow-hidden py-16 sm:py-20">
      <div className="absolute inset-0 bg-gradient-to-r from-orange-500 via-red-500 to-red-600" />
      <div className="pointer-events-none absolute -top-20 -left-20 h-64 w-64 rounded-full bg-white/20 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-24 -right-24 h-72 w-72 rounded-full bg-yellow-200/20 blur-3xl" />

      <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-8 flex items-center justify-between sm:mb-10">
          <h2 className="flex items-center gap-3 text-3xl font-black text-white sm:text-4xl">
            <svg className="h-8 w-8 text-yellow-200" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" />
            </svg>
            Flash Deals
          </h2>
          <span className="rounded-full bg-white/95 px-4 py-2 text-xs font-bold uppercase tracking-[0.18em] text-red-600 sm:px-6">
            Limited Time
          </span>
        </div>

        {status === 'loading' && (
          <div className="flex justify-center items-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-white"></div>
          </div>
        )}

        {status === 'failed' && (
          <div className="rounded-2xl bg-white/95 p-8 text-center shadow-xl">
            <p className="text-red-600 font-semibold">Failed to load flash deals.</p>
            <p className="text-sm text-gray-500 mt-2">{error}</p>
          </div>
        )}

        {status === 'succeeded' && renderSharedProductCards('flash-')}
      </div>
    </section>
  );

  return (
    <div className="min-h-screen bg-gray-50">
      <section className="relative h-[480px] sm:h-[560px] lg:h-[640px] overflow-hidden">
        {heroSlides.map((slide, index) => (
          <div
            key={slide.title}
            className={`absolute inset-0 transition-opacity duration-700 ${
              index === activeSlide ? 'opacity-100' : 'opacity-0 pointer-events-none'
            }`}
          >
            <img
              src={slide.image}
              alt={slide.title}
              className="h-full w-full object-cover"
              loading={index === 0 ? 'eager' : 'lazy'}
            />
            <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-black/20" />
            <div className="absolute inset-0 flex items-center">
              <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8">
                <div className="max-w-2xl text-white">
                  <p className="uppercase tracking-[0.2em] text-sm sm:text-base mb-3 text-orange-300">
                    New Season Collection
                  </p>
                  <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black leading-tight mb-4">
                    {slide.title}
                  </h1>
                  <p className="text-base sm:text-lg text-white/90 mb-8">{slide.subtitle}</p>
                  <div className="flex flex-col sm:flex-row gap-4">
                    <Link
                      to={slide.ctaLink}
                      className="inline-flex items-center justify-center rounded-full bg-orange-500 px-8 py-3 text-sm font-semibold text-white hover:bg-orange-400 transition-colors"
                    >
                      {slide.ctaLabel}
                    </Link>
                    <Link
                      to="/products/Smartphones"
                      className="inline-flex items-center justify-center rounded-full border border-white/70 px-8 py-3 text-sm font-semibold text-white hover:bg-white/10 transition-colors"
                    >
                      Shop Best Sellers
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}

        <button
          type="button"
          onClick={goToPreviousSlide}
          className="absolute left-3 sm:left-6 top-1/2 -translate-y-1/2 z-20 inline-flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full bg-black/40 text-white hover:bg-black/60 transition-colors"
          aria-label="Previous slide"
        >
          &#10094;
        </button>
        <button
          type="button"
          onClick={goToNextSlide}
          className="absolute right-3 sm:right-6 top-1/2 -translate-y-1/2 z-20 inline-flex h-10 w-10 sm:h-12 sm:w-12 items-center justify-center rounded-full bg-black/40 text-white hover:bg-black/60 transition-colors"
          aria-label="Next slide"
        >
          &#10095;
        </button>

        <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 flex gap-2">
          {heroSlides.map((slide, index) => (
            <button
              key={slide.title}
              type="button"
              onClick={() => goToSlide(index)}
              className={`h-2.5 rounded-full transition-all ${
                index === activeSlide ? 'w-7 bg-white' : 'w-2.5 bg-white/55 hover:bg-white/80'
              }`}
              aria-label={`Go to slide ${index + 1}`}
            />
          ))}
        </div>
      </section>

      <section className="relative overflow-hidden bg-gradient-to-b from-gray-50 via-white to-gray-100 py-16 sm:py-20">
        <div className="pointer-events-none absolute -left-24 top-10 h-56 w-56 rounded-full bg-indigo-200/35 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 bottom-10 h-64 w-64 rounded-full bg-orange-200/35 blur-3xl" />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mb-10 flex flex-col items-start justify-between gap-4 sm:mb-12 sm:flex-row sm:items-end">
            <div>
              <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-indigo-600">
                Explore Departments
              </p>
              <h2 className="text-3xl font-black text-gray-900 sm:text-4xl">Shop by Category</h2>
              <p className="mt-3 max-w-2xl text-base text-gray-600">
                Start with the category you love most and discover products curated for your style.
              </p>
            </div>
            <Link
              to="/products/Electronics"
              className="inline-flex items-center rounded-full border border-gray-300 bg-white px-6 py-2.5 text-sm font-semibold text-gray-700 transition hover:border-gray-400 hover:text-gray-900"
            >
              View All Categories
            </Link>
          </div>

          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-5">
            {categoryShowcase.map((category) => (
              <Link
                key={category.name}
                to={`/products/${category.slug}`}
                className="group relative isolate min-h-[330px] overflow-hidden rounded-3xl border border-white/30 bg-slate-900 shadow-lg shadow-slate-900/15 transition duration-300 hover:-translate-y-2 hover:shadow-2xl hover:shadow-slate-900/25"
              >
                <img
                  src={category.image}
                  alt={category.name}
                  className={`absolute inset-0 h-full w-full object-cover ${category.imagePosition} transition duration-500 group-hover:scale-110`}
                  loading="lazy"
                />
                <div className={`absolute inset-0 bg-gradient-to-b ${category.gradient}`} />
                <div className="absolute inset-0 bg-black/15" />

                <div className="absolute inset-x-3 bottom-3 rounded-2xl border border-white/20 bg-slate-900/45 p-4 backdrop-blur-md">
                  <div className="mb-3 flex items-start justify-between gap-3">
                    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold tracking-wide ${category.badgeShell} ${category.accentText}`}>
                      {category.badge}
                    </span>
                    <span className={`inline-flex h-10 w-10 items-center justify-center rounded-xl ${category.iconShell}`}>
                      <svg
                        className={`h-5 w-5 ${category.iconColor}`}
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        aria-hidden="true"
                      >
                        <path d={category.iconPath} />
                      </svg>
                    </span>
                  </div>
                  <h3 className="text-lg font-extrabold leading-tight text-white">{category.name}</h3>
                  <p className="mt-1.5 text-sm text-white/85">{category.description}</p>
                  <span className="mt-3 inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-white/90">
                    Shop now
                    <svg className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14m-6-6 6 6-6 6" />
                    </svg>
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {renderFlashDealsSection()}

      {/* Featured Products Section */}
      {renderProductGrid(
        'Featured Products',
        'Handpicked selections just for you. Discover our most popular items.'
      )}

      {/* Popular Products Section */}
      <section className="bg-gray-50">
        {renderProductGrid(
          'Popular Products',
          'Trending items loved by thousands of customers.'
        )}
      </section>
    </div>
  );
};
export default Home;