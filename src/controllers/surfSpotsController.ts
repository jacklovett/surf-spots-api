import { Request, Response } from 'express'
import SurfSpot, { ISurfSpot } from '../models/surfSpots'

// Create a new surf spot
export const create = async (req: Request, res: Response): Promise<void> => {
  try {
    const { country, region, name, description, longitude, latitude } = req.body
    const newSpot: ISurfSpot = new SurfSpot({
      country,
      region,
      name,
      description,
      longitude,
      latitude,
    })
    const savedSpot: ISurfSpot = await newSpot.save()
    res.status(201).json(savedSpot)
  } catch (error) {
    res.status(500).json({ message: 'Error creating a surf spot' })
  }
}

// Get all surf spots
export const getAll = async (req: Request, res: Response): Promise<void> => {
  try {
    const surfSpots: ISurfSpot[] = await SurfSpot.find()
    res.json(surfSpots)
  } catch (error) {
    res.status(500).json({ message: 'Error getting surf spots' })
  }
}

// Get a surf spot by ID
export const getById = async (req: Request, res: Response): Promise<void> => {
  const id: string = req.params.id
  try {
    const spot: ISurfSpot | null = await SurfSpot.findById(id)
    if (!spot) {
      res.status(404).json({ message: 'Surf spot not found' })
      return
    }
    res.json(spot)
  } catch (error) {
    res.status(500).json({ message: 'Error getting a surf spot by ID' })
  }
}

// Update a surf spot by ID
export const updateById = async (
  req: Request,
  res: Response,
): Promise<void> => {
  const id: string = req.params.id
  try {
    const updatedSpotData: ISurfSpot = req.body
    const updatedSpot: ISurfSpot | null = await SurfSpot.findByIdAndUpdate(
      id,
      updatedSpotData,
      { new: true },
    )
    if (!updatedSpot) {
      res.status(404).json({ message: 'Surf spot not found' })
      return
    }
    res.json(updatedSpot)
  } catch (error) {
    res.status(500).json({ message: 'Error updating a surf spot' })
  }
}

// Delete a surf spot by ID
export const deleteById = async (
  req: Request,
  res: Response,
): Promise<void> => {
  const id: string = req.params.id
  try {
    const deletedSpot: ISurfSpot | null = await SurfSpot.findByIdAndRemove(id)
    if (!deletedSpot) {
      res.status(404).json({ message: 'Surf spot not found' })
      return
    }
    res.json({ message: 'Surf spot deleted' })
  } catch (error) {
    res.status(500).json({ message: 'Error deleting a surf spot' })
  }
}
