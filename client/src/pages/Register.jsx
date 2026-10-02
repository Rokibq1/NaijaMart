import { useState } from "react";

function Register() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
  });

  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:5000/api/auth/register",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(formData),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        setMessage(data.message || "Registration failed.");
        setLoading(false);
        return;
      }

      setMessage("Registration successful! 🎉");

      setFormData({
        name: "",
        email: "",
        password: "",
      });
    } catch (error) {
      console.error("Registration error:", error);
      setMessage("Unable to connect to the server.");
    }

    setLoading(false);
  };

  return (
    <div className="auth-page">
      <div className="auth-box">

        <div className="auth-header">
          <h1>Create Account 🛍️</h1>

          <p>
            Join <strong>NaijaMart</strong> today
          </p>
        </div>

        <form
          onSubmit={handleSubmit}
          className="auth-form"
        >

          {/* Full Name */}
          <div className="form-group">
            <label htmlFor="name">
              Full Name
            </label>

            <input
              id="name"
              type="text"
              name="name"
              placeholder="Enter your full name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>

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
              placeholder="Minimum 8 characters"
              value={formData.password}
              onChange={handleChange}
              minLength="8"
              required
            />

            <small className="password-hint">
              Password must be at least 8 characters.
            </small>
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="auth-button"
            disabled={loading}
          >
            {loading
              ? "Creating account..."
              : "Create Account"}
          </button>

        </form>

        {/* Message */}
        {message && (
          <p className="auth-message">
            {message}
          </p>
        )}

        {/* Login link */}
        <div className="auth-footer">
          <p>
            Already have an account?{" "}
            <a href="/login">
              Login
            </a>
          </p>
        </div>

      </div>
    </div>
  );
}

export default Register;