import { useEffect, useState } from "react";

function Products({ addToCart }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("https://fakestoreapi.com/products")
      .then((response) => {
        if (!response.ok) {
          throw new Error("Failed to load products");
        }

        return response.json();
      })
      .then((data) => {
        setProducts(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error fetching products:", error);
        setError("Unable to load products.");
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <section className="products">
        <h2>Our Products</h2>
        <p className="loading">Loading products...</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="products">
        <h2>Our Products</h2>
        <p>{error}</p>
      </section>
    );
  }

  return (
    <section className="products">
      <h2>Our Products</h2>

      <div className="product-grid">
        {products.map((product) => (
          <div className="product-card" key={product.id}>
            
            <div className="product-image">
              <img
                src={product.image}
                alt={product.title}
                onError={(event) => {
                  event.target.src =
                    "https://placehold.co/300x300?text=NaijaMart";
                }}
              />
            </div>

            <h3>{product.title}</h3>

            <p className="product-price">
              ${product.price}
            </p>

            <button onClick={() => addToCart(product)}>
              Add to Cart
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Products;