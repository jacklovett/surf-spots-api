import express from 'express'
import cors from 'cors'

const app = express()
const port = process.env.PORT || 3001

app.use(cors({ origin: '*' })) // Allow all origins (for development only)

app.get('/', (req, res) => {
  res.send('Hello from the backend!')
})

app.listen(port, () => {
  console.log(`Server is running on port ${port}`)
})
