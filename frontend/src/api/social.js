import request from './request'

export const followUser = (userId) => request.post(`/social/follow/${userId}`)
export const unfollowUser = (userId) => request.delete(`/social/follow/${userId}`)
export const checkFollowStatus = (userId) => request.get(`/social/follow/status/${userId}`)
export const getFollowStats = (userId) => request.get(`/social/follow/stats/${userId}`)
export const getFollowers = (userId) => request.get(`/social/followers/${userId}`)
export const getFollowing = (userId) => request.get(`/social/following/${userId}`)

export const sharePlaylist = (playlistId) => request.post(`/social/playlist/share/${playlistId}`)
export const getSharedPlaylist = (shareToken) => request.get(`/social/playlist/shared/${shareToken}`)

export const shareSong = (songId) => request.post(`/social/song/share/${songId}`)
export const getSharedSong = (shareToken) => request.get(`/social/song/shared/${shareToken}`)

export const blockUser = (userId) => request.post(`/social/block/${userId}`)
export const unblockUser = (userId) => request.delete(`/social/block/${userId}`)
export const checkBlockStatus = (userId) => request.get(`/social/block/status/${userId}`)
