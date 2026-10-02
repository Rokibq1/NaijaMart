import { useNavigate } from "react-router-dom";

function Cart({ cart, setCart }) {
  const navigate = useNavigate();

  const increaseQuantity = (id) => {
    setCart((currentCart) =>
      currentCart.map((item) =>
        item.id === id
          ? {
              ...item,
              quantity: item.quantity + 1,
            }
          : item
      )
    );
  };

  const decreaseQuantity = (id) => {
    setCart((currentCart) =>
      currentCart
        .map((item) =>
          item.id === id
            ? {
                ...item,
                quantity: item.quantity - 1,
              }
            : item
        )
        .filter((item) => item.quantity > 0)
    );
  };

  const removeFromCart = (id) => {
    setCart((currentCart) =>
      currentCart.filter(
        (item) => item.id !== id
      )
    );
  };

  const total = cart.reduce(
    (sum, item) =>
      sum + item.price * item.quantity,
    0
  );

  const handleCheckout = () => {
    const token = localStorage.getItem("token");

    if (!token) {
      alert(
        "Please login before checking out."
      );

      navigate("/login");

      return;
    }

    if (cart.length === 0) {
      alert("Your cart is empty.");

      return;
    }

    // Go to checkout WITHOUT refreshing the page
    navigate("/checkout");
  };

  return (
    <section className="cart-page">

      <h1>
        Your Shopping Cart
      </h1>

      {cart.length === 0 ? (
        <p className="empty-cart">
          Your cart is currently empty.
        </p>
      ) : (
        <>
          <div className="cart-items">

            {cart.map((item) => (
              <div
                className="cart-item"
                key={item.id}
              >

                <img
                  src={item.image}
                  alt={item.title}
                />

                <div className="cart-item-info">

                  <h3>
                    {item.title}
                  </h3>

                  <p>
                    Price: ${item.price}
                  </p>

                  <div className="quantity-controls">

                    <button
                      onClick={() =>
                        decreaseQuantity(item.id)
                      }
                    >
                      -
                    </button>

                    <span>
                      {item.quantity}
                    </span>

                    <button
                      onClick={() =>
                        increaseQuantity(item.id)
                      }
                    >
                      +
                    </button>

                  </div>

                  <p>
                    Subtotal: $
                    {(
                      item.price *
                      item.quantity
                    ).toFixed(2)}
                  </p>

                  <button
                    className="remove-button"
                    onClick={() =>
                      removeFromCart(item.id)
                    }
                  >
                    Remove
                  </button>

                </div>

              </div>
            ))}

          </div>

          <div className="cart-total">

            <h2>
              Total: $
              {total.toFixed(2)}
            </h2>

            <button
              className="checkout-button"
              onClick={handleCheckout}
            >
              Checkout
            </button>

          </div>
        </>
      )}

    </section>
  );
}

export default Cart;