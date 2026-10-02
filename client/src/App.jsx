import { useEffect, useState } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";

import Navbar from "./components/Navbar";
import Hero from "./components/Hero";
import Categories from "./components/Categories";

import Home from "./pages/Home";
import ProductsPage from "./pages/ProductsPage";
import Cart from "./pages/Cart";
import Register from "./pages/Register";
import Login from "./pages/Login";
import Checkout from "./pages/Checkout";
import MyOrders from "./pages/MyOrders";

import "./App.css";

function App() {
  // ========================================
  // SHOPPING CART
  // ========================================
  const [cart, setCart] = useState([]);

  // ========================================
  // LOGGED-IN USER
  // ========================================
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem("user");

    if (savedUser) {
      try {
        return JSON.parse(savedUser);
      } catch (error) {
        return null;
      }
    }

    return null;
  });

  // ========================================
  // LISTEN FOR LOGIN
  // ========================================
  useEffect(() => {
    const handleLogin = () => {
      const savedUser =
        localStorage.getItem("user");

      if (savedUser) {
        try {
          setUser(JSON.parse(savedUser));
        } catch (error) {
          setUser(null);
        }
      }
    };

    window.addEventListener(
      "userLoggedIn",
      handleLogin
    );

    return () => {
      window.removeEventListener(
        "userLoggedIn",
        handleLogin
      );
    };
  }, []);

  // ========================================
  // LOGOUT
  // ========================================
  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");

    setUser(null);
  };

  // ========================================
  // ADD PRODUCT TO CART
  // ========================================
  const addToCart = (product) => {
    setCart((currentCart) => {
      const existingProduct =
        currentCart.find(
          (item) => item.id === product.id
        );

      // If product already exists,
      // increase its quantity
      if (existingProduct) {
        return currentCart.map((item) =>
          item.id === product.id
            ? {
                ...item,
                quantity:
                  item.quantity + 1,
              }
            : item
        );
      }

      // Otherwise add new product
      return [
        ...currentCart,
        {
          ...product,
          quantity: 1,
        },
      ];
    });
  };

  // ========================================
  // APP
  // ========================================
  return (
    <BrowserRouter>

      {/* ====================================
          NAVBAR
      ===================================== */}
      <Navbar
        cartCount={cart.length}
        user={user}
        onLogout={handleLogout}
      />

      {/* ====================================
          ROUTES
      ===================================== */}
      <Routes>

        {/* ==================================
            HOME
        =================================== */}
        <Route
          path="/"
          element={
            <Home>
              <Hero />
              <Categories />
            </Home>
          }
        />

        {/* ==================================
            PRODUCTS
        =================================== */}
        <Route
          path="/products"
          element={
            <ProductsPage
              addToCart={addToCart}
            />
          }
        />

        {/* ==================================
            CART
        =================================== */}
        <Route
          path="/cart"
          element={
            <Cart
              cart={cart}
              setCart={setCart}
            />
          }
        />

        {/* ==================================
            CHECKOUT
        =================================== */}
        <Route
          path="/checkout"
          element={
            <Checkout
              cart={cart}
              setCart={setCart}
            />
          }
        />

        {/* ==================================
            MY ORDERS
        =================================== */}
        <Route
          path="/my-orders"
          element={<MyOrders />}
        />

        {/* ==================================
            REGISTER
        =================================== */}
        <Route
          path="/register"
          element={<Register />}
        />

        {/* ==================================
            LOGIN
        =================================== */}
        <Route
          path="/login"
          element={<Login />}
        />

      </Routes>

    </BrowserRouter>
  );
}

export default App;