import { useEffect, useState } from "react";

function MyOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");

  // ===============================
  // LOAD CUSTOMER ORDERS
  // ===============================
  useEffect(() => {
    const fetchOrders = async () => {
      const token = localStorage.getItem("token");

      if (!token) {
        setMessage(
          "Please login to view your orders."
        );

        setLoading(false);

        return;
      }

      try {
        const response = await fetch(
          "http://localhost:5000/api/orders/my-orders",
          {
            method: "GET",

            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        const data = await response.json();

        if (!response.ok) {
          setMessage(
            data.message ||
              "Unable to load your orders."
          );

          setLoading(false);

          return;
        }

        setOrders(data.orders);

      } catch (error) {
        console.error(
          "Get orders error:",
          error
        );

        setMessage(
          "Unable to connect to the server."
        );
      }

      setLoading(false);
    };

    fetchOrders();
  }, []);

  // ===============================
  // LOADING
  // ===============================
  if (loading) {
    return (
      <section className="orders-page">
        <div className="orders-container">

          <h1>
            My Orders
          </h1>

          <p>
            Loading your orders...
          </p>

        </div>
      </section>
    );
  }

  // ===============================
  // NOT LOGGED IN / ERROR
  // ===============================
  if (message) {
    return (
      <section className="orders-page">
        <div className="orders-container">

          <h1>
            My Orders
          </h1>

          <p className="orders-message">
            {message}
          </p>

        </div>
      </section>
    );
  }

  return (
    <section className="orders-page">

      <div className="orders-container">

        {/* =========================
            PAGE HEADER
        ========================== */}
        <div className="orders-header">

          <h1>
            My Orders
          </h1>

          <p>
            View your previous orders
            and track your deliveries.
          </p>

        </div>

        {/* =========================
            NO ORDERS
        ========================== */}
        {orders.length === 0 ? (

          <div className="no-orders">

            <h2>
              No Orders Yet
            </h2>

            <p>
              You haven't placed any
              orders yet.
            </p>

          </div>

        ) : (

          /* =========================
             ORDERS
          ========================== */
          <div className="orders-list">

            {orders.map((order) => (

              <div
                className="order-card"
                key={order._id}
              >

                {/* Order Header */}
                <div className="order-card-header">

                  <div>

                    <h2>
                      Order #{order._id.slice(-8)}
                    </h2>

                    <p>
                      Placed on{" "}
                      {new Date(
                        order.createdAt
                      ).toLocaleDateString()}
                    </p>

                  </div>

                  <span
                    className={`order-status status-${order.status
                      .toLowerCase()
                      .replaceAll(" ", "-")}`}
                  >
                    {order.status}
                  </span>

                </div>

                {/* Order Products */}
                <div className="order-products">

                  {order.items.map(
                    (item, index) => (

                      <div
                        className="order-product"
                        key={`${order._id}-${index}`}
                      >

                        {/* Product Image */}
                        <img
                          src={item.image}
                          alt={item.title}
                        />

                        {/* Product Details */}
                        <div className="order-product-info">

                          <h3>
                            {item.title}
                          </h3>

                          <p>
                            Quantity:{" "}
                            {item.quantity}
                          </p>

                          <p>
                            Price: $
                            {item.price}
                          </p>

                        </div>

                        {/* Product Total */}
                        <strong>
                          $
                          {(
                            item.price *
                            item.quantity
                          ).toFixed(2)}
                        </strong>

                      </div>

                    )
                  )}

                </div>

                {/* Order Footer */}
                <div className="order-card-footer">

                  <div>

                    <span>
                      Total
                    </span>

                    <strong>
                      $
                      {Number(
                        order.totalAmount
                      ).toFixed(2)}
                    </strong>

                  </div>

                  <div>

                    <span>
                      Payment
                    </span>

                    <strong>
                      {order.paymentStatus}
                    </strong>

                  </div>

                </div>

                {/* Tracking Information */}
                <div className="tracking-info">

                  <h3>
                    Delivery Tracking
                  </h3>

                  <p>
                    Status:{" "}
                    <strong>
                      {order.status}
                    </strong>
                  </p>

                  <p>
                    Tracking Number:{" "}
                    <strong>
                      {order.trackingNumber ||
                        "Not assigned yet"}
                    </strong>
                  </p>

                  <p>
                    Courier:{" "}
                    <strong>
                      {order.courier ||
                        "Not assigned yet"}
                    </strong>
                  </p>

                </div>

              </div>

            ))}

          </div>

        )}

      </div>

    </section>
  );
}

export default MyOrders;