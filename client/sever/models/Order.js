const mongoose = require("mongoose");

const orderSchema = new mongoose.Schema(
  {
    // ===============================
    // CUSTOMER
    // ===============================
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },

    // ===============================
    // PRODUCTS
    // ===============================
    items: [
      {
        productId: {
          type: String,
          required: true,
        },

        title: {
          type: String,
          required: true,
        },

        price: {
          type: Number,
          required: true,
        },

        quantity: {
          type: Number,
          required: true,
          min: 1,
        },

        image: {
          type: String,
        },
      },
    ],

    // ===============================
    // TOTAL PRICE
    // ===============================
    totalAmount: {
      type: Number,
      required: true,
    },

    // ===============================
    // DELIVERY INFORMATION
    // ===============================
    delivery: {
      fullName: {
        type: String,
        required: true,
      },

      phone: {
        type: String,
        required: true,
      },

      address: {
        type: String,
        required: true,
      },

      city: {
        type: String,
        required: true,
      },

      state: {
        type: String,
        required: true,
      },
    },

    // ===============================
    // ORDER STATUS
    // ===============================
    status: {
      type: String,
      enum: [
        "Pending",
        "Confirmed",
        "Processing",
        "Shipped",
        "In Transit",
        "Out for Delivery",
        "Delivered",
        "Cancelled",
      ],
      default: "Pending",
    },

    // ===============================
    // TRACKING INFORMATION
    // ===============================
    trackingNumber: {
      type: String,
      default: null,
    },

    courier: {
      type: String,
      default: null,
    },

    // ===============================
    // PAYMENT STATUS
    // ===============================
    paymentStatus: {
      type: String,
      enum: [
        "Pending",
        "Paid",
        "Failed",
      ],
      default: "Pending",
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model(
  "Order",
  orderSchema
);