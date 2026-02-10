import React, { useEffect, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  clearSuccessMessage as clearWishlistSuccess,
  selectWishlistSuccessMessage,
} from '../features/wishlist/wishlistSlice';

const TOAST_DURATION_MS = 2600;

const WishlistToast = () => {
  const dispatch = useDispatch();
  const successMessage = useSelector(selectWishlistSuccessMessage);

  const toastType = useMemo(() => {
    if (!successMessage) return null;

    const normalized = successMessage.toLowerCase();
    if (normalized.includes('added to wishlist')) return 'added';
    if (normalized.includes('removed from wishlist')) return 'removed';
    return null;
  }, [successMessage]);

  useEffect(() => {
    if (!toastType) return;

    const clearTimer = setTimeout(() => dispatch(clearWishlistSuccess()), TOAST_DURATION_MS);

    return () => {
      clearTimeout(clearTimer);
    };
  }, [toastType, dispatch]);

  if (!toastType) return null;

  const isAdded = toastType === 'added';
  const toastText = isAdded ? 'Added to wishlist!' : 'Removed from wishlist';
  const cardClass = isAdded
    ? 'border-green-200 bg-green-50/95 text-green-700'
    : 'border-blue-200 bg-blue-50/95 text-blue-600';
  const iconShellClass = isAdded ? 'bg-green-600 text-white' : 'bg-blue-600 text-white';

  return (
    <div className="pointer-events-none fixed right-4 top-24 z-[90] w-[calc(100%-2rem)] sm:w-auto sm:max-w-xs">
      <div
        className={`animate-toastSlideDown rounded-lg border px-3 py-2 shadow-lg backdrop-blur-md ${cardClass}`}
        role="status"
        aria-live="polite"
      >
        <div className="flex items-center gap-2.5">
          <span className={`inline-flex h-6 w-6 items-center justify-center rounded-full ${iconShellClass}`}>
            {isAdded ? (
              <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
              </svg>
            ) : (
              <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            )}
          </span>
          <p className="text-base font-semibold leading-tight">{toastText}</p>
        </div>
      </div>
    </div>
  );
};

export default WishlistToast;
