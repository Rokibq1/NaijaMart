import { useState } from "react";

function Login() {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  // Handle input changes
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // Handle login
  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:5000/api/auth/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(formData),
        }
      );

      const data = await response.json();

      // Login failed
      if (!response.ok) {
        setMessage(data.message || "Login failed.");
        setLoading(false);
        return;
      }

      // Save JWT token
      localStorage.setItem("token", data.token);

      // Save user information
      localStorage.setItem(
        "user",
        JSON.stringify(data.user)
      );

      // Tell App.jsx that the user has logged in
      window.dispatchEvent(
        new Event("userLoggedIn")
      );

      // Show success message
      setMessage("Login successful! 🎉");

      // Clear form
      setFormData({
        email: "",
        password: "",
      });
    } catch (error) {
      console.error("Login error:", error);

      setMessage(
        "Unable to connect to the server."
      );
    }

    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-box">

        {/* Header */}
        <div className="auth-header">
          <h1>Welcome Back 👋</h1>

          <p>
            Login to your{" "}
            <strong>NaijaMart</strong> account
          </p>
        </div>

        {/* Login Form */}
        <form
          onSubmit={handleSubmit}
          className="auth-form"
        >

          {/* Email */}
          <div className="form-group">
            <label htmlFor="email">
              Email Address
            </label>

            <input
              id="email"
              type="email"
              name="email"
              placeholder="Enter your email"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          {/* Password */}
          <div className="form-group">
            <label htmlFor="password">
              Password
            </label>

            <input
              id="password"
              type="password"
              name="password"
              placeholder="Enter your password"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          {/* Login Button */}
          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading
              ? "Logging in..."
              : "Login"}
          </button>

        </form>

        {/* Message */}
        {message && (
          <p className="auth-message">
            {message}
          </p>
        )}

        {/* Register Link */}
        <div className="auth-footer">
          <p>
            Don't have an account?{" "}
            <a href="/register">
              Create one
            </a>
          </p>
        </div>

      </div>
    </div>
  );
}

export default Login;