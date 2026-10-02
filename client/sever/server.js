const express = require("express");
const mongoose = require("mongoose");
const cors = require("cors");
require("dotenv").config();

// ========================================
// IMPORT ROUTES
// ========================================
const authRoutes = require("./routes/authRoutes");
const orderRoutes = require("./routes/orderRoutes");

// ========================================
// CREATE EXPRESS APP
// ========================================
const app = express();

const PORT = process.env.PORT || 5000;

// ========================================
// MIDDLEWARE
// ========================================
app.use(cors());

app.use(express.json());

// ========================================
// AUTH ROUTES
// ========================================
app.use(
  "/api/auth",
  authRoutes
);

// ========================================
// ORDER ROUTES
// ========================================
app.use(
  "/api/orders",
  orderRoutes
);

// ========================================
// HOME / TEST ROUTE
// ========================================
app.get("/", (req, res) => {
  res.json({
    message:
      "NaijaMart API is running successfully!",
  });
});

// ========================================
// CONNECT TO MONGODB
// ========================================
mongoose
  .connect(process.env.MONGO_URI)

  .then(() => {
    console.log(
      "MongoDB connected successfully"
    );

    // ====================================
    // START SERVER
    // ====================================
    app.listen(PORT, () => {
      console.log(
        `NaijaMart server running on port ${PORT}`
      );
    });
  })

  .catch((error) => {
    console.error(
      "MongoDB connection failed:",
      error.message
    );
  });