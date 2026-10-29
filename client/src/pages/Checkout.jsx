import { useState } from "react";

function Checkout({ cart, setCart }) {
  const [formData, setFormData] = useState({
    fullName: "",
    phone: "",
    address: "",
    city: "",
    state: "",
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  // ========================================
  // HANDLE FORM INPUT
  // ========================================
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // ========================================
  // CALCULATE TOTAL
  // ========================================
  const total = cart.reduce(
    (sum, item) =>
      sum + item.price * item.quantity,
    0
  );

  // ========================================
  // PLACE ORDER
  // ========================================
  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage("");

    const token = localStorage.getItem("token");

    // Check login
    if (!token) {
      setMessage(
        "Please login before placing an order."
      );

      return;
    }

    // Check cart
    if (cart.length === 0) {
      setMessage("Your cart is empty.");

      return;
    }

    // Check delivery information
    if (
      !formData.fullName.trim() ||
      !formData.phone.trim() ||
      !formData.address.trim() ||
      !formData.city.trim() ||
      !formData.state.trim()
    ) {
      setMessage(
        "Please fill in all delivery information."
      );

      return;
    }

    setLoading(true);

    try {
      // ====================================
      // PREPARE ORDER PRODUCTS
      // ====================================
      const orderItems = cart.map((item) => ({
        productId: String(item.id),
        title: item.title,
        price: item.price,
        quantity: item.quantity,
        image: item.image,
      }));

      // ====================================
      // SEND ORDER TO BACKEND
      // ====================================
      const response = await fetch(
        "http://localhost:5000/api/orders",
        {
          method: "POST",

          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },

          body: JSON.stringify({
            items: orderItems,
            totalAmount: total,
            delivery: formData,
          }),
        }
      );

      const data = await response.json();

      // ====================================
      // HANDLE ERROR
      // ====================================
      if (!response.ok) {
        setMessage(
          data.message ||
            "Unable to place order."
        );

        setLoading(false);

        return;
      }

      // ====================================
      // ORDER SUCCESSFUL
      // ====================================
      setMessage(
        "Order placed successfully! 🎉"
      );

      console.log(
        "Created order:",
        data.order
      );

      // ====================================
      // CLEAR CART
      // ====================================
      setCart([]);

      // ====================================
      // CLEAR FORM
      // ====================================
      setFormData({
        fullName: "",
        phone: "",
        address: "",
        city: "",
        state: "",
      });

      // ====================================
      // SAVE LAST ORDER
      // ====================================
      localStorage.setItem(
        "lastOrder",
        JSON.stringify(data.order)
      );

    } catch (error) {
      console.error(
        "Checkout error:",
        error
      );

      setMessage(
        "Unable to connect to the server. Make sure your backend is running."
      );
    }

    setLoading(false);
  };

  return (
    <section className="checkout-page">

      <div className="checkout-container">

        {/* ==================================
            HEADER
        =================================== */}
        <div className="checkout-header">

          <h1>
            Checkout
          </h1>

          <p>
            Enter your delivery information
          </p>

        </div>

        <div className="checkout-content">

          {/* ==================================
              DELIVERY FORM
          =================================== */}
          <div className="checkout-form-box">

            <h2>
              Delivery Information
            </h2>

            <form
              onSubmit={handleSubmit}
            >

              {/* FULL NAME */}
              <div className="checkout-field">

                <label htmlFor="fullName">
                  Full Name
                </label>

                <input
                  id="fullName"
                  type="text"
                  name="fullName"
                  placeholder="Enter your full name"
                  value={formData.fullName}
                  onChange={handleChange}
                />

              </div>

              {/* PHONE */}
              <div className="checkout-field">

                <label htmlFor="phone">
                  Phone Number
                </label>

                <input
                  id="phone"
                  type="tel"
                  name="phone"
                  placeholder="Enter your phone number"
                  value={formData.phone}
                  onChange={handleChange}
                />

              </div>

              {/* ADDRESS */}
              <div className="checkout-field">

                <label htmlFor="address">
                  Delivery Address
                </label>

                <textarea
                  id="address"
                  name="address"
                  placeholder="Enter your full delivery address"
                  value={formData.address}
                  onChange={handleChange}
                  rows="4"
                />

              </div>

              {/* CITY */}
              <div className="checkout-field">

                <label htmlFor="city">
                  City
                </label>

                <input
                  id="city"
                  type="text"
                  name="city"
                  placeholder="Enter your city"
                  value={formData.city}
                  onChange={handleChange}
                />

              </div>

              {/* STATE */}
              <div className="checkout-field">

                <label htmlFor="state">
                  State
                </label>

                <input
                  id="state"
                  type="text"
                  name="state"
                  placeholder="Enter your state"
                  value={formData.state}
                  onChange={handleChange}
                />

              </div>

              {/* ==================================
                  PLACE ORDER BUTTON
              =================================== */}
              <button
                type="submit"
                className="place-order-button"
                disabled={loading}
              >
                {loading
                  ? "Placing Order..."
                  : "Place Order"}
              </button>

            </form>

            {/* ==================================
                MESSAGE
            =================================== */}
            {message && (
              <p className="checkout-message">
                {message}
              </p>
            )}

          </div>

          {/* ==================================
              ORDER SUMMARY
          =================================== */}
          <div className="order-summary">

            <h2>
              Order Summary
            </h2>

            {cart.length === 0 ? (
              <p>
                Your cart is empty.
              </p>
            ) : (
              <>
                {cart.map((item) => (
                  <div
                    className="summary-item"
                    key={item.id}
                  >

                    <div className="summary-item-info">

                      <h3>
                        {item.title}
                      </h3>

                      <p>
                        Quantity:{" "}
                        {item.quantity}
                      </p>

                    </div>

                    <strong>
                      $
                      {(
                        item.price *
                        item.quantity
                      ).toFixed(2)}
                    </strong>

                  </div>
                ))}

                <div className="summary-total">

                  <span>
                    Total
                  </span>

                  <strong>
                    ${total.toFixed(2)}
                  </strong>

                </div>
              </>
            )}

          </div>

        </div>

      </div>

    </section>
  );
}

export default Checkout;