# Mini Island 2D - Server

## 📖 Overview
This is the server application for Mini Island 2D, a real-time multiplayer game server. It handles client connections, game logic, player authentication, database management, and real-time communication via WebSocket.

## 🎮 Features
- **WebSocket Server** - Real-time bidirectional communication with clients
- **User Authentication** - Secure login/registration with password hashing (jBCrypt)
- **Database Management** - MySQL integration with connection pooling
- **Game Logic**:
  - Maze generation and management
  - PvP battle system
  - Player state synchronization
  - Collision detection
- **Leaderboard System** - Real-time player rankings
- **Multi-threaded Architecture** - Handle multiple concurrent clients
- **Server GUI** - Management interface for server monitoring
- **Data Persistence** - User accounts, scores, and game statistics

## 🛠️ Technology Stack
- **Java 17** - Modern Java LTS version
- **Java-WebSocket 1.5.3** - WebSocket server library
- **MySQL 8.0.33** - Relational database
- **Apache Commons DBCP 2.11.0** - Database connection pooling
- **jBCrypt 0.4** - Password hashing and verification
- **Maven** - Build and dependency management

## 📁 Project Structure
```
mini-island-2d-server/
├── src/
│   ├── server/                   # Main server components
│   │   ├── Main.java            # Server entry point
│   │   ├── Server.java          # Main server class
│   │   ├── WebSocketGameServer.java  # WebSocket handler
│   │   ├── ServerGUI.java       # Server management UI
│   │   └── Protocol.java        # Communication protocol
│   ├── dao/                      # Data Access Objects
│   │   └── UserDAO.java         # User database operations
│   ├── databaseConnect/          # Database connection
│   │   └── DatabaseConnection.java  # Connection pool manager
│   ├── service/                  # Business logic services
│   │   ├── AuthService.java     # Authentication service
│   │   ├── GameService.java     # Game logic service
│   │   └── LeaderboardService.java  # Ranking system
│   ├── map/                      # Map generation and management
│   │   └── MazeGen.java         # Maze generator
│   └── testShop/                 # Test/example modules
│       ├── server/              # Test server
│       └── client/              # Test client
├── Resource/
│   └── lib/                      # External JAR libraries
│       └── commons-dbcp2-2.11.0.jar
└── pom.xml                       # Maven configuration
```

## 🚀 Getting Started

### Prerequisites
- **Java 17 JDK** or higher ([Download](https://adoptium.net/temurin/releases/?version=17))
- **MySQL Database** 5.7+ or 8.0+
- **Maven** (optional, for command-line builds)

### Database Setup

1. **Install MySQL** and start the service

2. **Create database and tables:**
   ```sql
   CREATE DATABASE IF NOT EXISTS `miniisland`;
   USE `miniisland`;

   CREATE TABLE IF NOT EXISTS `users` (
     `id` int(11) NOT NULL AUTO_INCREMENT,
     `username` varchar(255) NOT NULL UNIQUE,
     `email` varchar(255) NOT NULL UNIQUE,
     `password_hash` varchar(255) NOT NULL,
     `coins` int(11) NOT NULL DEFAULT 0,
     `points` int(11) NOT NULL DEFAULT 0,
     `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     INDEX `idx_points` (`points` DESC),
     INDEX `idx_username` (`username`)
   ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
   ```

3. **Configure database connection** in `src/databaseConnect/DatabaseConnection.java`:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/miniisland";
   private static final String USER = "root";
   private static final String PASSWORD = "your_password";
   ```

### Installation

1. **Navigate to server directory:**
   ```bash
   cd mini-island-2d-server
   ```

2. **Build the project:**
   ```bash
   mvn clean compile
   ```

3. **Run the server:**
   ```bash
   mvn exec:java -Dexec.mainClass="server.Main"
   ```

   Or run directly using Java:
   ```bash
   java -cp target/classes server.Main
   ```

### Server Configuration

Configure server settings in the main server class:

- **Port**: Default WebSocket port is `8887`
- **Database**: MySQL connection settings
- **Max Connections**: Configure thread pool size
- **Game Settings**: Maze size, spawn points, game rules

## 🎯 Server Operations

### Starting the Server

```bash
# Using Maven
mvn exec:java -Dexec.mainClass="server.Main"

# Using compiled classes
java -cp target/classes server.Main
```

### Server GUI

The server includes a GUI for management:
- View connected clients
- Monitor server status
- View real-time logs
- Stop/restart server
- Database status

### Monitoring

The server logs important events:
- Client connections/disconnections
- Authentication attempts
- Game events
- Database operations
- Error messages

## 🔧 Development

### Building from Source

```bash
# Clean build
mvn clean compile

# Package as JAR
mvn package

# Run with dependencies
mvn exec:java -Dexec.mainClass="server.Main"

# Skip tests
mvn package -DskipTests
```

### Database Operations

The server uses DAO pattern for database access:

```java
// Example: UserDAO operations
UserDAO userDao = new UserDAO();

// Create user
User user = userDao.createUser(username, email, hashedPassword);

// Get user
User user = userDao.getUserByUsername(username);

// Update points
userDao.updateUserPoints(userId, newPoints);

// Get leaderboard
List<User> topPlayers = userDao.getTopPlayers(20);
```

### Adding New Features

- **New game modes** - Add to `src/service/GameService.java`
- **New API endpoints** - Extend `src/server/Protocol.java`
- **Database tables** - Update schema and create new DAOs
- **Server commands** - Add to `src/server/ServerGUI.java`

## 📦 Dependencies

```xml
<!-- Java-WebSocket -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>

<!-- MySQL Connector -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>

<!-- Apache Commons DBCP -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.11.0</version>
</dependency>

<!-- jBCrypt -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

## 🔒 Security

### Password Security
- Passwords are hashed using jBCrypt (BCrypt algorithm)
- Salted hashes prevent rainbow table attacks
- Never store plain-text passwords

### Connection Security
- WebSocket connections (consider upgrading to WSS for production)
- Input validation on all client messages
- SQL injection prevention via prepared statements
- Rate limiting for authentication attempts

### Best Practices
- Change default database credentials
- Use environment variables for sensitive data
- Enable MySQL SSL connections for production
- Regular security audits and updates

## 📡 Network Protocol

### Message Format
All messages use JSON format:

```json
{
  "type": "message_type",
  "data": { ... }
}
```

### Message Types

#### Authentication
```json
// Login
{"type": "login", "username": "player1", "password": "hashed_password"}

// Register
{"type": "register", "username": "player1", "email": "player@example.com", "password": "password"}

// Response
{"type": "auth_response", "success": true, "userId": 123, "token": "..."}
```

#### Game Events
```json
// Player movement
{"type": "move", "playerId": 123, "x": 100, "y": 200, "direction": "up"}

// State update (server -> clients)
{"type": "state_update", "players": [...], "timestamp": 1234567890}

// Game action
{"type": "action", "playerId": 123, "action": "bomb", "x": 100, "y": 200}
```

#### Chat
```json
// Send message
{"type": "chat", "message": "Hello!"}

// Broadcast message
{"type": "chat_message", "username": "player1", "message": "Hello!", "timestamp": 1234567890}
```

#### Leaderboard
```json
// Request
{"type": "leaderboard_request"}

// Response
{"type": "leaderboard", "players": [{"rank": 1, "username": "...", "points": 1000}, ...]}
```

## 🐛 Troubleshooting

### Server won't start
- **Port already in use**: Change port or kill existing process
- **Java version**: Verify Java 17+ with `java -version`
- **Dependencies**: Run `mvn clean install` to resolve dependencies

### Database connection issues
- **Check MySQL service**: Ensure MySQL is running
- **Verify credentials**: Double-check username/password
- **Test connection**: Use MySQL Workbench or command line
- **Check firewall**: Ensure port 3306 is accessible
- **Connection pool**: Check DBCP configuration

### Client connection problems
- **Firewall**: Allow port 8887 (or configured port)
- **WebSocket protocol**: Verify client uses correct `ws://` URL
- **Logs**: Check server logs for connection errors

### Performance issues
- **Connection pooling**: Optimize DBCP settings
- **Thread pool**: Increase executor thread count
- **Database indexes**: Ensure proper indexing on `users` table
- **Memory**: Increase JVM heap size: `java -Xmx2G -Xms1G ...`

### Memory leaks
- Monitor connections and ensure proper cleanup
- Check for unclosed database connections
- Use profiler tools (JVisualVM, JProfiler)

## 🧪 Testing

### Test Server
A test shop server is included for development:

```bash
mvn exec:java -Dexec.mainClass="testShop.server.ShopServer"
```

### Manual Testing
1. Start the server
2. Connect with test client or game client
3. Test authentication flow
4. Verify game mechanics
5. Check database persistence

### Load Testing
- Use multiple client instances
- Monitor CPU and memory usage
- Check database connection pool
- Verify message throughput

## 🚀 Deployment

### Production Checklist
- [ ] Change database credentials
- [ ] Enable MySQL SSL
- [ ] Configure firewall rules
- [ ] Set up logging to file
- [ ] Configure backup strategy
- [ ] Monitor server resources
- [ ] Set up auto-restart on crash
- [ ] Use environment variables for config
- [ ] Enable WSS (WebSocket Secure)
- [ ] Set up reverse proxy (nginx)

### Docker Deployment (Optional)
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/mini-island-2d-server-1.0-SNAPSHOT.jar app.jar
EXPOSE 8887
CMD ["java", "-jar", "app.jar"]
```

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL UNIQUE,
  `email` varchar(255) NOT NULL UNIQUE,
  `password_hash` varchar(255) NOT NULL,
  `coins` int(11) NOT NULL DEFAULT 0,
  `points` int(11) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_points` (`points` DESC),
  INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Write/update tests
5. Update documentation
6. Submit a pull request

## 📄 License

This project is part of the Mini Island 2D game system.

## 🔗 Related

- **Client**: See `../mini-island-2d/README.md`
- **Main README**: See `../README.md` for complete project overview

## 👥 Support

For issues, questions, or contributions, please refer to the main repository.

---

**Note**: This server must be running before clients can connect to play the game.
