import express from 'express'
import mongoose from 'mongoose'
import cors from 'cors'
import surfSpotRoutes from './routes/surfSpotRoutes'

const app = express()
const port = process.env.PORT || 3001

mongoose
  .connect('mongodb://localhost:27017/surf-spots')
  .then(() => {
    console.log('Connected to MongoDB')
  })
  .catch((err) => {
    console.error('Error connecting to MongoDB:', err)
  })

app.use(cors({ origin: '*' })) // Allow all origins (for development only)

// Use the surf spot routes
app.use('/', surfSpotRoutes)

app.listen(port, () => console.log(`Server is running on port ${port}`))
