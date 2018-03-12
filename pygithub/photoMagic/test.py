import cv2
import numpy as np
import p2m
img1=cv2.imread('1.jpeg')
img2=cv2.imread('2.jpeg')
img=p2m.addweight(img1,img2,0.5,0.5)
cv2.imshow('image',img)
cv2.waitKey()
cv2.imwrite('new.jpeg',img)
