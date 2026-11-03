# personalized-product-recommendation-system-built-with-Java-AWT
A personalized product recommendation system built with Java AWT. Customers can log in, search products, shop and view orders, while an admin panel manages the catalogue. Recommendations are ranked by age, gender, season, purchase history and search history, each with a clear reason. Data is stored in simple CSV files, no database needed.
Security: passwords are stored as hashes, never as plain text.
Roles: Customer and Administrator, each with its own screens.
Architecture: three packages: app (data and storage), engine (recommendation logic) and gui (screens).
Inventory: stock reduces automatically at checkout, and low-stock items are flagged for the admin.
An admin panel lets staff add and update products, track stock and view all orders.
