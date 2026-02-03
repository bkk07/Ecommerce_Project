-- Rating Service Database Initialization Script

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS ecommerce_ratings;
USE ecommerce_ratings;

-- Ratings table
CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    message VARCHAR(1000),
    is_verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_sku (sku),
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_sku_user (sku, user_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_order_sku (order_id, sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rating eligibility table
CREATE TABLE IF NOT EXISTS rating_eligibility (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    can_rate BOOLEAN NOT NULL DEFAULT TRUE,
    has_rated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    rated_at TIMESTAMP NULL,
    
    INDEX idx_eligibility_user_sku (user_id, sku),
    INDEX idx_eligibility_order (order_id),
    INDEX idx_pending_ratings (user_id, can_rate, has_rated),
    UNIQUE KEY uk_eligibility_order_sku (order_id, sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant permissions (for development)
-- In production, use least privilege principle
GRANT ALL PRIVILEGES ON ecommerce_ratings.* TO 'root'@'%';
FLUSH PRIVILEGES;
