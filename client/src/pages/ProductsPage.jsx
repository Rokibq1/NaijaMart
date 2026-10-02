import Products from "../components/Products";

function ProductsPage({ addToCart }) {
  return (
    <Products addToCart={addToCart} />
  );
}

export default ProductsPage;