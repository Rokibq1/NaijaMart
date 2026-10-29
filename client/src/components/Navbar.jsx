import { Link } from "react-router-dom";

function Navbar({ cartCount, user, onLogout }) {
  return (
    <nav className="navbar">

      {/* =========================
          LOGO
      ========================== */}
      <div className="logo">
        <Link to="/">
          NaijaMart
        </Link>
      </div>

      {/* =========================
          NAVIGATION LINKS
      ========================== */}
      <div className="nav-links">

        <Link to="/">
          Home
        </Link>

        <Link to="/products">
          Products
        </Link>

        <Link to="/categories">
          Categories
        </Link>

        <Link to="/cart">
          Cart ({cartCount})
        </Link>

        {/* Show My Orders only when logged in */}
        {user && (
          <Link to="/my-orders">
            My Orders
          </Link>
        )}

      </div>

      {/* =========================
          USER ACTIONS
      ========================== */}
      <div className="nav-actions">

        {user ? (
          <>
            <span className="welcome-user">
              Hi, {user.name}
            </span>

            <button
              className="logout-button"
              onClick={onLogout}
            >
              Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login">
              Login
            </Link>

            <Link to="/register">
              Sign Up
            </Link>
          </>
        )}

      </div>

    </nav>
  );
}

export default Navbar;