const mongoose = require("mongoose");

const userSchema = new mongoose.Schema(
  {
    // ========================================
    // USER NAME
    // ========================================
    name: {
      type: String,
      required: true,
      trim: true,
    },

    // ========================================
    // USER EMAIL
    // ========================================
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
    },

    // ========================================
    // USER PASSWORD
    // ========================================
    password: {
      type: String,
      required: true,
      minlength: 8,
    },

    // ========================================
    // USER ROLE
    // ========================================
    role: {
      type: String,
      enum: ["customer", "admin"],
      default: "customer",
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model(
  "User",
  userSchema
);