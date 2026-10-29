function Hero() {
  return (
    <section className="hero">
      <div className="hero-content">
        <p className="hero-small-text">WELCOME TO NAIJAMART</p>

        <h1>
          Shop Smart.
          <br />
          Shop Easy.
        </h1>

        <p className="hero-description">
          Discover amazing products at great prices.
          Shop from the comfort of your home.
        </p>

        <div className="hero-buttons">
          <button>Shop Now</button>
          <button className="secondary-button">
            Explore Products
          </button>
        </div>
      </div>
    </section>
  );
}

export default Hero;