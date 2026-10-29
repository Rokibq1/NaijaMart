package com.naijamart.config;

import com.naijamart.entity.Product;
import com.naijamart.repository.ProductRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner loadProducts(ProductRepository repository) {
        return args -> {
            addProduct(repository, "Samsung Galaxy Smartphone",
                    "A modern smartphone for everyday use.",
                    "250000.00", 15,
                    "https://placehold.co/400x300?text=Samsung+Phone");

            addProduct(repository, "HP Laptop",
                    "A laptop suitable for work, school, and business.",
                    "650000.00", 8,
                    "https://placehold.co/400x300?text=HP+Laptop");

            addProduct(repository, "Wireless Headphones",
                    "Wireless headphones for music and calls.",
                    "45000.00", 20,
                    "https://placehold.co/400x300?text=Headphones");

            addProduct(repository, "Smart Watch",
                    "A smartwatch for notifications and fitness tracking.",
                    "80000.00", 12,
                    "https://placehold.co/400x300?text=Smart+Watch");

            addProduct(repository, "Bluetooth Speaker",
                    "A portable speaker for music at home or outdoors.",
                    "35000.00", 18,
                    "https://placehold.co/400x300?text=Speaker");

            addProduct(repository, "Power Bank",
                    "Portable backup power for your devices.",
                    "25000.00", 30,
                    "https://placehold.co/400x300?text=Power+Bank");

            addProduct(repository, "Wireless Mouse",
                    "A wireless mouse for laptops and desktop computers.",
                    "12000.00", 25,
                    "https://placehold.co/400x300?text=Wireless+Mouse");

            addProduct(repository, "Mechanical Keyboard",
                    "A keyboard for work, study, and gaming.",
                    "40000.00", 10,
                    "https://placehold.co/400x300?text=Keyboard");

            addProduct(repository, "Tablet",
                    "A tablet for reading, entertainment, and productivity.",
                    "220000.00", 7,
                    "https://placehold.co/400x300?text=Tablet");

            addProduct(repository, "USB Flash Drive",
                    "Portable storage for documents and files.",
                    "8000.00", 40,
                    "https://placehold.co/400x300?text=Flash+Drive");

            addProduct(repository, "Phone Charger",
                    "A charging adapter for compatible mobile devices.",
                    "10000.00", 35,
                    "https://placehold.co/400x300?text=Phone+Charger");

            addProduct(repository, "Laptop Backpack",
                    "A backpack for carrying a laptop and accessories.",
                    "30000.00", 14,
                    "https://placehold.co/400x300?text=Backpack");

            System.out.println("NaijaMart product initialization finished.");
        };
    }

    private void addProduct(
            ProductRepository repository,
            String name,
            String description,
            String price,
            int stock,
            String imageUrl) {

        boolean exists = repository.findAll().stream()
                .anyMatch(product ->
                        product.getName().equalsIgnoreCase(name));

        if (exists) {
            System.out.println("Product already exists: " + name);
            return;
        }

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stock);
        product.setImageUrl(imageUrl);
        product.setActive(true);

        repository.save(product);
        System.out.println("Added product: " + name);
    }
}