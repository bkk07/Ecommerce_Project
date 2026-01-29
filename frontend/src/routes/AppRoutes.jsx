import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Home from '../pages/Home';
import Products from '../pages/Products';
import Checkout from '../pages/Checkout';
import SearchResults from '../pages/SearchResults';

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/products/:category" element={<Products />} />
      <Route path="/checkout" element={<Checkout />} />
      <Route path="/search" element={<SearchResults />} />
    </Routes>
  );
};

export default AppRoutes;
