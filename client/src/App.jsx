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
const [cart, setCart] = useState([]);

const [user, setUser] = useState(() => {
const savedUser = localStorage.getItem("user");


if (savedUser) {
  try {
    return JSON.parse(savedUser);
  } catch {
    return null;
  }
}

return null;


});

useEffect(() => {
const handleLogin = () => {
const savedUser = localStorage.getItem("user");


  if (savedUser) {
    try {
      setUser(JSON.parse(savedUser));
    } catch {
      setUser(null);
    }
  }
};

window.addEventListener("userLoggedIn", handleLogin);

return () => {
  window.removeEventListener("userLoggedIn", handleLogin);
};


}, []);

const handleLogout = () => {
localStorage.removeItem("token");
localStorage.removeItem("user");
setUser(null);
};

const addToCart = (product) => {
setCart((currentCart) => {
const existingProduct = currentCart.find(
(item) => item.id === product.id
);


  if (existingProduct) {
    return currentCart.map((item) =>
      item.id === product.id
        ? {
            ...item,
            quantity: item.quantity + 1,
          }
        : item
    );
  }

  return [
    ...currentCart,
    {
      ...product,
      quantity: 1,
    },
  ];
});


};

return ( <BrowserRouter> <Navbar
     cartCount={cart.length}
     user={user}
     onLogout={handleLogout}
   />


  <Routes>
    <Route
      path="/"
      element={
        <Home>
          <Hero />
          <Categories />
        </Home>
      }
    />

    <Route
      path="/products"
      element={
        <ProductsPage
          addToCart={addToCart}
        />
      }
    />

    <Route
      path="/cart"
      element={
        <Cart
          cart={cart}
          setCart={setCart}
        />
      }
    />

    <Route
      path="/checkout"
      element={
        <Checkout
          cart={cart}
          setCart={setCart}
        />
      }
    />

    <Route
      path="/my-orders"
      element={<MyOrders />}
    />

    <Route
      path="/register"
      element={<Register />}
    />

    <Route
      path="/login"
      element={<Login />}
    />
  </Routes>
</BrowserRouter>


);
}

export default App;
