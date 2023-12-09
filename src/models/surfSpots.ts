import { Document, Schema, model } from 'mongoose'

export interface ISurfSpot extends Document {
  country: string
  region: string
  name: string
  description: string
  longitude: number
  latitude: number
}

const surfSpotSchema = new Schema<ISurfSpot>({
  country: String,
  region: String,
  name: String,
  description: String,
  longitude: Number,
  latitude: Number,
})

const SurfSpot = model<ISurfSpot>('SurfSpot', surfSpotSchema)

export default SurfSpot
