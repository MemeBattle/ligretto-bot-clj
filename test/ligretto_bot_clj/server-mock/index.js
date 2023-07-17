import { createServer } from "node:http";
import { Server } from "socket.io";

const httpServer = createServer();
const io = new Server(httpServer, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
  },
  transports: ['websocket', 'polling'],
});

io.on('connection', (socket) => {
  console.log(`Socket ${socket.id} connected`);
  socket.emit('connected', { message: 'connected' });

  socket.on('join', (roomId) => {
    const currentRooms = Object.keys(socket.rooms).filter((room) => room !== socket.id);
    currentRooms.forEach((room) => socket.leave(room));

    socket.join(roomId);
    console.log(`Socket ${socket.id} joined the room: ${roomId}`);
  });

  socket.on('leave', (roomId) => {
    socket.leave(roomId);
    console.log(`Socket ${socket.id} left the room: ${roomId}`);
  });

  socket.on('disconnect', () => {
    console.log('user disconnected');
  });
});

httpServer.on('error', (err) => {
  console.error(err);
})

httpServer.on('request', (req) => {
  console.log(req.url);
});

httpServer.listen(process.env.PORT ?? 3000, () => {
  console.log('listening on *:3000');
});

process.on('SIGTERM', () => {
  io.close(() => {
    console.log('Server closed SIGTERM')
    process.exit()
  })
})

process.on('SIGINT', () => {
  io.close(() => {
    console.log('Server closed SIGINT')
    process.exit()
  })
})
