function Categories() {
  const categories = [
    "Fashion",
    "Electronics",
    "Shoes",
    "Home & Living",
    "Computers",
    "Accessories",
  ];

  return (
    <section className="categories">
      <h2>Shop By Category</h2>

      <div className="category-grid">
        {categories.map((category) => (
          <div className="category-card" key={category}>
            <div className="category-icon">🛍️</div>
            <h3>{category}</h3>
          </div>
        ))}
      </div>
    </section>
  );
}

export default Categories;