const express = require("express");
const jwt = require("jsonwebtoken");

const Order = require("../models/Order");

const router = express.Router();

// ========================================
// AUTHENTICATION MIDDLEWARE
// ========================================
const protect = (req, res, next) => {
  try {
    // Get token from request header
    const authHeader = req.headers.authorization;

    if (!authHeader) {
      return res.status(401).json({
        message: "Authentication required.",
      });
    }

    // Expected format:
    // Bearer YOUR_TOKEN
    const token = authHeader.split(" ")[1];

    if (!token) {
      return res.status(401).json({
        message: "Invalid authentication token.",
      });
    }

    // Verify token
    const decoded = jwt.verify(
      token,
      process.env.JWT_SECRET
    );

    // Save user information
    req.user = decoded;

    next();
  } catch (error) {
    console.error(
      "Authentication error:",
      error.message
    );

    return res.status(401).json({
      message: "Invalid or expired token.",
    });
  }
};

// ========================================
// CREATE ORDER
// ========================================
router.post("/", protect, async (req, res) => {
  try {
    const {
      items,
      totalAmount,
      delivery,
    } = req.body;

    // Check items
    if (!items || items.length === 0) {
      return res.status(400).json({
        message: "Your order must contain products.",
      });
    }

    // Check delivery information
    if (
      !delivery ||
      !delivery.fullName ||
      !delivery.phone ||
      !delivery.address ||
      !delivery.city ||
      !delivery.state
    ) {
      return res.status(400).json({
        message:
          "Please provide complete delivery information.",
      });
    }

    // Create order
    const order = await Order.create({
      user: req.user.userId,

      items,

      totalAmount,

      delivery,

      status: "Pending",

      paymentStatus: "Pending",
    });

    res.status(201).json({
      message: "Order created successfully!",

      order: {
        id: order._id,
        items: order.items,
        totalAmount: order.totalAmount,
        delivery: order.delivery,
        status: order.status,
        trackingNumber:
          order.trackingNumber,
        courier: order.courier,
        paymentStatus:
          order.paymentStatus,
        createdAt: order.createdAt,
      },
    });
  } catch (error) {
    console.error(
      "Create order error:",
      error
    );

    res.status(500).json({
      message:
        "Something went wrong while creating the order.",
    });
  }
});

// ========================================
// GET MY ORDERS
// ========================================
router.get("/my-orders", protect, async (req, res) => {
  try {
    const orders = await Order.find({
      user: req.user.userId,
    }).sort({
      createdAt: -1,
    });

    res.json({
      orders,
    });
  } catch (error) {
    console.error(
      "Get orders error:",
      error
    );

    res.status(500).json({
      message:
        "Something went wrong while loading your orders.",
    });
  }
});

// ========================================
// GET ONE ORDER
// ========================================
router.get("/:id", protect, async (req, res) => {
  try {
    const order = await Order.findOne({
      _id: req.params.id,
      user: req.user.userId,
    });

    if (!order) {
      return res.status(404).json({
        message: "Order not found.",
      });
    }

    res.json({
      order,
    });
  } catch (error) {
    console.error(
      "Get order error:",
      error
    );

    res.status(500).json({
      message:
        "Something went wrong while loading the order.",
    });
  }
});

// ========================================
// EXPORT ROUTER
// ========================================
module.exports = router;