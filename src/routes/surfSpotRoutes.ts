// routes/surfSpotRoutes.ts
import { Router } from 'express'
import * as surfSpotController from './../controllers/surfSpotsController'

const router = Router()

// Create a new surf spot
router.post('/surf-spots', surfSpotController.create)

// Get all surf spots
router.get('/surf-spots', surfSpotController.getAll)

// Get a specific surf spot by ID
router.get('/surf-spots/:id', surfSpotController.getById)

// Update a surf spot by ID
router.put('/surf-spots/:id', surfSpotController.updateById)

// Delete a surf spot by ID
router.delete('/surf-spots/:id', surfSpotController.deleteById)

export default router
